package dev.adventurers.forge.worldgen;

import com.mojang.serialization.MapCodec;
import dev.adventurers.core.api.Numbers;
import dev.adventurers.core.world.*;
import net.minecraft.core.*;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Generates actual overworld blocks. It never reads mutable civilization state from a chunk worker. */
public final class PlanetChunkGenerator extends ChunkGenerator {
    public static final MapCodec<PlanetChunkGenerator> CODEC = PlanetBiomeSource.CODEC.xmap(PlanetChunkGenerator::new,g->g.source);
    private final PlanetBiomeSource source;
    public PlanetChunkGenerator(PlanetBiomeSource source) { super(source); this.source=source; }
    public TerrainSettings settings() { return source.settings(); }
    public TerrainAtlas atlas(long seed) { return source.atlas(seed); }
    @Override protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }
    @Override public ChunkPos getOrigin(RandomState random) {
        var site = atlas(random.seed()).spawnSite();
        return new ChunkPos(Math.floorDiv(site.x(), 16), Math.floorDiv(site.z(), 16));
    }
    @Override public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structures,RandomState random,long seed) {
        atlas(random.seed()); return super.createState(structures,random,seed);
    }
    @Override public CompletableFuture<ChunkAccess> createBiomes(RandomState random,Blender blender,StructureManager structures,ChunkAccess chunk) {
        atlas(random.seed()); return super.createBiomes(random,blender,structures,chunk);
    }
    @Override public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk,Blender blender,RandomState random,
            StructureManager structures,BiomeManager biomes,WorldGenRegion carverRegion,Set<Holder<Biome>> possibleBiomes) {
        var atlas=atlas(random.seed()); var pos=new BlockPos.MutableBlockPos();
        var floor=chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        var surface=chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
            int wx=chunk.getPos().getMinBlockX()+x,wz=chunk.getPos().getMinBlockZ()+z;
            var column=atlas.column(wx+.5,wz+.5);
            int top=Math.min(chunk.getMaxY(),Math.max(column.ground(),column.water()));
            for(int y=Math.max(getMinY(),chunk.getMinY());y<=top;y++) {
                var state=block(atlas,wx,y,wz,column);
                if(!state.isAir()) {
                    chunk.setBlockState(pos.set(wx,y,wz),state);
                    floor.update(x,y,z,state);surface.update(x,y,z,state);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }
    public static BlockState block(TerrainAtlas atlas,int x,int y,int z,TerrainAtlas.Column column) {
        if(y<TerrainAtlas.MIN_Y||y>=TerrainAtlas.MIN_Y+TerrainAtlas.HEIGHT)return Blocks.AIR.defaultBlockState();
        if(y==TerrainAtlas.MIN_Y)return Blocks.BEDROCK.defaultBlockState();
        if(y>column.ground()) {
            if(y>column.water())return Blocks.AIR.defaultBlockState();
            if(y==column.water()&&column.temperature()<0)return Blocks.ICE.defaultBlockState();
            return Blocks.WATER.defaultBlockState();
        }
        if(atlas.cave(x+.5,y,z+.5,column))return (y<-54?Blocks.LAVA:Blocks.AIR).defaultBlockState();
        if(y==column.ground())return switch(column.biome()) {
            case OCEAN,FROZEN_OCEAN,RIVER,FROZEN_RIVER->Blocks.GRAVEL.defaultBlockState();
            case BEACH,DESERT->Blocks.SAND.defaultBlockState();
            case SNOWY_PLAINS->Blocks.SNOW_BLOCK.defaultBlockState();
            case STONY_PEAKS->Blocks.STONE.defaultBlockState();
            default->Blocks.GRASS_BLOCK.defaultBlockState();
        };
        if(y>=column.ground()-3) return (column.biome()==TerrainAtlas.Biome.DESERT||column.biome()==TerrainAtlas.Biome.BEACH?Blocks.SANDSTONE:Blocks.DIRT).defaultBlockState();
        // Plate mineralization adds small deterministic veins; biome features add vanilla ores and vegetation.
        var point=PlanetCoordinates.normalize(x+.5,z+.5,atlas.settings());
        long vein=Numbers.mix(atlas.seed()^Numbers.mix(Math.floorDiv((int)Math.floor(point.x()),4))
                ^Long.rotateLeft(Numbers.mix(Math.floorDiv((int)Math.floor(point.z()),4)),21)^Numbers.mix(Math.floorDiv(y,4)));
        if((vein&1023)<2+column.ore()*10)return (y<0?Blocks.DEEPSLATE_IRON_ORE:(vein&1024)==0?Blocks.COPPER_ORE:Blocks.COAL_ORE).defaultBlockState();
        return (y<0?Blocks.DEEPSLATE:Blocks.STONE).defaultBlockState();
    }
    @Override public int getBaseHeight(int x,int z,Heightmap.Types type,LevelHeightAccessor height,RandomState random) {
        var atlas=atlas(random.seed());var c=atlas.column(x+.5,z+.5);
        for(int y=Math.min(height.getMaxY(),Math.max(c.ground(),c.water()));y>=Math.max(height.getMinY(),getMinY());y--)
            if(type.isOpaque().test(block(atlas,x,y,z,c)))return y+1;
        return height.getMinY();
    }
    @Override public NoiseColumn getBaseColumn(int x,int z,LevelHeightAccessor height,RandomState random) {
        var atlas=atlas(random.seed());var c=atlas.column(x+.5,z+.5);var blocks=new BlockState[height.getHeight()];
        for(int i=0;i<blocks.length;i++)blocks[i]=block(atlas,x,height.getMinY()+i,z,c);
        return new NoiseColumn(height.getMinY(),blocks);
    }
    @Override public int getGenDepth() { return TerrainAtlas.HEIGHT; }
    @Override public int getMinY() { return TerrainAtlas.MIN_Y; }
    @Override public int getSeaLevel() { return TerrainAtlas.SEA_LEVEL; }
    @Override public int getSpawnHeight(LevelHeightAccessor height) { return TerrainAtlas.SEA_LEVEL+2; }
    @Override public void addDebugScreenInfo(List<String> lines,RandomState random,BlockPos feet,SamplerContext context) {
        var c=atlas(random.seed()).column(feet.getX(),feet.getZ());
        lines.add("Adventurers planet: "+settings().circumference()+" x "+settings().poleDistance()+" / "+c.biome()+" / "+Math.round(c.temperature())+" C");
    }
    @Override public void spawnOriginalMobs(WorldGenRegion region) {
        var center=region.getCenter();var random=new WorldgenRandom(new LegacyRandomSource(region.getSeed()));
        random.setDecorationSeed(region.getSeed(),center.getMinBlockX(),center.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(region,center.getWorldPosition().atY(region.getMaxY()),center,random);
    }
}
