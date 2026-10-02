package dev.adventurers.forge;

import dev.adventurers.core.civilization.*;
import dev.adventurers.core.life.Citizen;
import dev.adventurers.core.world.LoadingTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Bounded vanilla villager shells and component-built geometry for loaded cities. */
public final class WorldProjection {
    public static final String TAG = "adventurers.citizen.";
    private final ServerRuntime runtime;
    private final Path ledgerPath;
    private final Properties ledger = new Properties();
    public WorldProjection(ServerRuntime runtime, Path ledgerPath) throws IOException {
        this.runtime = runtime; this.ledgerPath = ledgerPath;
        if (Files.exists(ledgerPath)) try(var reader=Files.newBufferedReader(ledgerPath)){ledger.load(reader);}
    }
    public void tick() {
        var level = runtime.server().overworld();
        var shells = new HashMap<Long,Villager>();
        for(var entity:level.getAllEntities()) if(entity instanceof Villager villager) {
            long id=citizenId(villager);
            if(id>0) { var duplicate=shells.putIfAbsent(id,villager); if(duplicate!=null)villager.discard(); }
        }
        var retained = new HashSet<Long>();
        for(var city:runtime.world().cities()) {
            if(runtime.world().tier(city)!=LoadingTier.HOT || city.population()==0) continue;
            int visible=0;
            for(var person:city.citizens()) {
                if(!person.alive() || visible++>=ModConfig.VISIBLE_CITIZENS.get())continue;
                retained.add(person.id());
                var entity=shells.get(person.id());
                if(entity==null) {
                    int dx=(int)(person.id()%7)-3,dz=(int)((person.id()/7)%7)-3;
                    var pos=safeSurface(city.x()+dx*2,city.z()+dz*2);
                    if(pos==null)continue;
                    entity=EntityTypes.VILLAGER.create(level,EntitySpawnReason.COMMAND);
                    if(entity==null)continue;
                    entity.snapTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
                    entity.addTag(TAG+person.id());entity.setPersistenceRequired();
                    // Vanilla AI is an interaction shell; all persistent decisions/resources live in the core.
                    level.addFreshEntity(entity);
                }
                entity.setCustomName(Component.literal(city.name()+" · "+person.id()+" · "+person.role()));
                entity.setCustomNameVisible(true);
            }
            if(ModConfig.BUILD_IN_WORLD.get()) for(var building:city.buildings())projectBuilding(city,building);
        }
        for(var entry:shells.entrySet()) if(!retained.contains(entry.getKey()))entry.getValue().discard();
    }
    public static long citizenId(Entity entity) {
        for(String tag:entity.entityTags()) if(tag.startsWith(TAG)) {
            try{return Long.parseLong(tag.substring(TAG.length()));}catch(NumberFormatException ignored){return -1;}
        }
        return -1;
    }
    public BlockPos safeSurface(int x,int z) {
        var level=runtime.server().overworld();
        if(!level.hasChunk(x>>4,z>>4))return null;
        var pos=new BlockPos(x,level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
        if(!level.getBlockState(pos.below()).getFluidState().isEmpty())return null;
        if(!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir())return null;
        return pos;
    }
    public void placePlayer(ServerPlayer player,City city) {
        var level=runtime.server().overworld();
        // Spawn within the chosen civilization; callers cannot choose the exact coordinate.
        var random=new Random(runtime.world().seed()^player.getUUID().getLeastSignificantBits()^runtime.world().tick());
        for(int i=0;i<64;i++) {
            int x=city.x()+random.nextInt(49)-24,z=city.z()+random.nextInt(49)-24;
            level.getChunk(x>>4,z>>4);
            var pos=safeSurface(x,z);
            if(pos!=null){player.teleportTo(level,x+.5,pos.getY(),z+.5,Set.of(),player.getYRot(),player.getXRot(),true);return;}
        }
        throw new IllegalStateException("所选城邦附近暂未找到安全落点，请选择其他文明");
    }
    private void projectBuilding(City city,Building building) {
        if(building.stage()==Building.Stage.RUIN)return;
        String key=Long.toString(building.id());
        int projected=Integer.parseInt(ledger.getProperty(key+".stage","-1"));
        int completed=building.stage().ordinal()-1;
        if(projected>=completed)return;
        double angle=building.id()*2.399963229728653;
        int x=city.x()+(int)(Math.cos(angle)*(14+building.id()%4*9));
        int z=city.z()+(int)(Math.sin(angle)*(14+building.id()%4*9));
        var level=runtime.server().overworld();
        if(!level.hasChunk(x>>4,z>>4)||!level.hasChunk((x+building.width())>>4,(z+building.depth())>>4))return;
        String savedY=ledger.getProperty(key+".y");
        var surface=savedY==null?safeSurface(x,z):new BlockPos(x,Integer.parseInt(savedY),z);
        if(surface==null)return;
        int y=surface.getY();
        int stage=projected+1; // At most one small construction stage per second per building.
        for(int dx=0;dx<building.width();dx++)for(int dz=0;dz<building.depth();dz++) {
            boolean edge=dx==0||dz==0||dx==building.width()-1||dz==building.depth()-1;
            boolean corner=(dx==0||dx==building.width()-1)&&(dz==0||dz==building.depth()-1);
            boolean door=dx==building.width()/2&&dz==0;
            if(stage==0)place(new BlockPos(x+dx,y-1,z+dz),Blocks.COBBLESTONE.defaultBlockState());
            if(stage==1&&corner)for(int h=0;h<3;h++)place(new BlockPos(x+dx,y+h,z+dz),Blocks.OAK_LOG.defaultBlockState());
            if(stage==2&&edge&&!corner&&!door)for(int h=0;h<3;h++)if(!(h==1&&(dx==building.width()/2||dz==building.depth()/2)))place(new BlockPos(x+dx,y+h,z+dz),Blocks.OAK_PLANKS.defaultBlockState());
            if(stage==3)place(new BlockPos(x+dx,y+3,z+dz),Blocks.OAK_SLAB.defaultBlockState());
            if(stage==4&&edge&&!door&&!corner&&(dx==building.width()/2||dz==building.depth()/2))place(new BlockPos(x+dx,y+1,z+dz),Blocks.GLASS_PANE.defaultBlockState());
            if(stage==5&&!edge)place(new BlockPos(x+dx,y-1,z+dz),Blocks.OAK_PLANKS.defaultBlockState());
        }
        ledger.setProperty(key+".stage",Integer.toString(stage));ledger.setProperty(key+".y",Integer.toString(y));
    }
    private void place(BlockPos pos,BlockState state) {
        var level=runtime.server().overworld();
        if(level.getBlockState(pos).isAir()||level.getBlockState(pos).canBeReplaced())level.setBlock(pos,state,3);
    }
    public void save() throws IOException {
        Files.createDirectories(ledgerPath.getParent());
        Path temp=Files.createTempFile(ledgerPath.getParent(),"projection-",".tmp");
        try {
            try(var writer=Files.newBufferedWriter(temp)){ledger.store(writer,"Adventurers projected stages; prevents rebuilding player-modified structures");}
            try{Files.move(temp,ledgerPath,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temp,ledgerPath,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
}
