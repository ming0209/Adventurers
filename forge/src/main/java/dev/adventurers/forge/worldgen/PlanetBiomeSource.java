package dev.adventurers.forge.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.adventurers.core.world.*;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.*;
import java.util.*;
import java.util.stream.Stream;

public final class PlanetBiomeSource extends BiomeSource {
    public static final MapCodec<PlanetBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("region_size").forGetter(source -> source.settings.regionSize()),
            Codec.INT.optionalFieldOf("terrain_version",1).forGetter(source -> source.settings.version()),
            Biome.CODEC.listOf().fieldOf("biomes").forGetter(source -> source.palette)
    ).apply(instance,PlanetBiomeSource::new));
    private final TerrainSettings settings;
    private final List<Holder<Biome>> palette;
    private volatile TerrainAtlas atlas;
    public PlanetBiomeSource(int regionSize,int version,List<Holder<Biome>> palette) {
        settings = new TerrainSettings(regionSize,version);
        if(palette.size()!=TerrainAtlas.Biome.values().length)throw new IllegalArgumentException("Planet biome palette is incomplete");
        this.palette = List.copyOf(palette);
    }
    public TerrainSettings settings() { return settings; }
    public synchronized TerrainAtlas atlas(long seed) {
        if(atlas==null||atlas.seed()!=seed)atlas=new TerrainAtlas(seed,settings);
        return atlas;
    }
    public Holder<Biome> biome(TerrainAtlas atlas,int x,int z) { return palette.get(atlas.column(x,z).biome().ordinal()); }
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() { return palette.stream(); }
    @Override protected MapCodec<? extends BiomeSource> codec() { return CODEC; }
    @Override public BiomeResolver createResolver(Climate.Sampler sampler) {
        // The generator binds the level seed before biome generation and structure lookup.
        var terrain=atlas;
        if(terrain==null)throw new IllegalStateException("Planet biome source has not been bound to the level seed");
        return (x,y,z)->biome(terrain,x*4,z*4);
    }
}
