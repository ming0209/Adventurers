package dev.adventurers.core;

import dev.adventurers.core.civilization.City;
import dev.adventurers.core.persistence.*;
import dev.adventurers.core.world.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

final class WorldGenerationTests {
    static List<SimulationTests.Case> cases() {
        return List.of(
                new SimulationTests.Case("planet coordinates cross longitude and both poles",WorldGenerationTests::coordinates),
                new SimulationTests.Case("geology is seed deterministic and has drainage and climate diversity",WorldGenerationTests::geology),
                new SimulationTests.Case("terrain, water and caves match across chart boundaries",WorldGenerationTests::boundaries),
                new SimulationTests.Case("parallel chunk sampling is independent of exploration order",WorldGenerationTests::parallel),
                new SimulationTests.Case("settlements choose dry buildable terrain and share region coordinates",WorldGenerationTests::settlements),
                new SimulationTests.Case("all three planet sizes retain geographic scale",WorldGenerationTests::sizes),
                new SimulationTests.Case("new planet genesis and save continuation agree",WorldGenerationTests::genesis),
                new SimulationTests.Case("released v1 saves migrate without moving cities or losing players",WorldGenerationTests::legacy)
        );
    }
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    private static TerrainAtlas atlas(long seed) { return new TerrainAtlas(seed,new TerrainSettings(96)); }
    private static WorldModel world(TerrainAtlas atlas) {
        var world=new WorldModel(atlas.seed(),atlas.simulationPlanet(),Laws.overworld(),4,4096);
        world.attachTerrain(atlas);return world;
    }
    private static void coordinates() {
        var settings=new TerrainSettings(96);double w=settings.circumference(),h=settings.poleDistance();
        var east=PlanetCoordinates.normalize(w/2+7,12,settings);
        check(east.x()==-w/2+7&&east.z()==12&&!east.reflected(),"east wrap");
        var north=PlanetCoordinates.normalize(17,h/2+9,settings);
        check(north.z()==h/2-9&&north.x()==17-w/2&&north.reflected(),"pole must change longitude, not torus wrap");
        var south=PlanetCoordinates.normalize(17,-h/2-9,settings);
        check(south.z()==-h/2+9&&south.x()==north.x()&&south.reflected(),"opposite pole");
        check(PlanetCoordinates.normalize(17+w*100,23+h*200,settings).equals(new PlanetCoordinates.Point(17,23,false)),"multi-orbit normalization");
        boolean rejected=false;try{PlanetCoordinates.normalize(Double.NaN,0,settings);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"nonfinite coordinates accepted");
    }
    private static void geology() {
        var a=atlas(42);var same=atlas(42);var other=atlas(77);var biomes=EnumSet.noneOf(TerrainAtlas.Biome.class);int differences=0;
        for(int z=-550;z<=550;z+=19)for(int x=-1100;x<=1100;x+=19) {
            var c=a.column(x,z);check(c.equals(same.column(x,z)),"same seed changed");biomes.add(c.biome());
            if(!c.equals(other.column(x,z)))differences++;
            check(c.ground()>TerrainAtlas.MIN_Y&&c.ground()<280&&Double.isFinite(c.moisture()),"invalid geology");
        }
        var stats=a.statistics();
        check(stats.landCells()>200&&stats.oceanCells()>200&&stats.riverCells()>20,"missing continents or drainage");
        check(!a.rivers().isEmpty()&&biomes.size()>=6&&differences>100,"missing diversity");
        check(a.column(0,575).temperature()<0,"polar climate is not cold");
    }
    private static void boundaries() {
        var atlas=atlas(2026);var s=atlas.settings();
        for(int z=-500;z<=500;z+=31)for(int x=-1100;x<=1100;x+=137) {
            var c=atlas.column(x+.5,z+.5);
            check(c.equals(atlas.column(x+.5+s.circumference(),z+.5)),"longitude mismatch");
            check(atlas.cave(x+.5,-20,z+.5,c)==atlas.cave(x+.5+s.circumference(),-20,z+.5,c),"cave boundary mismatch");
        }
        for(int x=-1100;x<=1100;x+=61) {
            var outside=atlas.column(x,s.poleDistance()/2.0+7);
            var inside=atlas.column(x+s.circumference()/2.0,s.poleDistance()/2.0-7);
            check(outside.equals(inside),"pole terrain mismatch");
            check(Math.abs(atlas.column(x,s.poleDistance()/2.0-1).ground()-atlas.column(x+s.circumference()/2.0,s.poleDistance()/2.0-1).ground())<=4,"polar terrain cliff");
        }
    }
    private static void parallel() throws Exception {
        var atlas=atlas(55);var expected=new ArrayList<TerrainAtlas.Column>();
        for(int i=0;i<600;i++)expected.add(atlas.column(i*23-5000,i*11-1600));
        try(var pool=Executors.newFixedThreadPool(4)) {
            var work=new ArrayList<Callable<TerrainAtlas.Column>>();
            for(int i=599;i>=0;i--){int sample=i;work.add(()->atlas.column(sample*23-5000,sample*11-1600));}
            var results=pool.invokeAll(work);
            for(int i=0;i<600;i++)check(results.get(i).get().equals(expected.get(599-i)),"worker order changed terrain");
        }
    }
    private static void settlements() {
        var atlas=atlas(42);var world=world(atlas);int suitable=0;
        for(int i=0;i<288;i++) {
            var site=atlas.settlement(i);if(site.isEmpty())continue;suitable++;
            var s=site.orElseThrow();var c=atlas.column(s.x(),s.z());
            check(!c.submerged()&&PlanetCoordinates.region(s.x(),s.z(),atlas.settings())==i,"site not dry or not in region");
            if(world.cities().isEmpty()) {
                var city=world.foundCity(world.planet().region(i));
                check(city.x()==s.x()&&city.z()==s.z(),"city ignored terrain placement");
            }
        }
        check(suitable>5,"insufficient buildable regions");
    }
    private static void sizes() {
        for(int size:new int[]{96,192,384}) {
            var atlas=new TerrainAtlas(9,new TerrainSettings(size));
            check(atlas.settings().circumference()==24*size&&atlas.settings().poleDistance()==12*size,"size mismatch");
            check(atlas.simulationPlanet().regions().size()==288,"simulation regions depend on block count");
            var spawn=atlas.spawnSite();
            check(!atlas.column(spawn.x(),spawn.z()).submerged(),"new player spawn starts in water");
        }
        boolean rejected=false;try{new TerrainSettings(128);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"unsupported geography silently accepted");
    }
    private static void genesis() throws Exception {
        for(long seed:new long[]{42,77,2026}) {
            var atlas=atlas(seed);var sim=new Simulation(world(atlas));var initial=atlas.column(17,33);
            sim.generate(City.Era.STONE,2048);
            check(sim.world().population()>0,"planet civilization failed for seed "+seed);
            for(var city:sim.world().cities())check(!atlas.column(city.x(),city.z()).submerged(),"civilization founded under water");
            check(initial.equals(atlas.column(17,33)),"mutable simulation changed generated terrain");
            var loaded=new Simulation(WorldCodec.decode(WorldCodec.encode(sim.world())));
            sim.advanceDays(24);loaded.advanceDays(24);
            check(Arrays.equals(WorldCodec.encode(sim.world()),WorldCodec.encode(loaded.world())),"geographic save continuation diverged");
        }
    }
    private static void legacy() throws Exception {
        byte[] old;
        try(var stream=WorldGenerationTests.class.getResourceAsStream("/world-v1.bin")) {
            check(stream!=null,"released-format fixture missing");old=stream.readAllBytes();
        }
        var world=WorldCodec.decode(old);var city=world.cities().iterator().next();
        check(java.nio.ByteBuffer.wrap(old).getInt(4)==1,"fixture was regenerated with the current encoder");
        check(world.seed()==77&&world.population()==16&&world.terrain().isEmpty(),"legacy world was converted into a planet");
        check(city.technology()==2.4&&city.magic()==3.5&&world.player(new UUID(0,77)).isPresent(),"legacy identity or progress lost");
        int x=city.x(),z=city.z();var loaded=WorldCodec.decode(WorldCodec.encode(world));
        check(loaded.city(city.id()).orElseThrow().x()==x&&loaded.city(city.id()).orElseThrow().z()==z,"legacy cities moved");
        var dir=Files.createTempDirectory("adventurers-v1-");var path=dir.resolve("world.bin");
        try {
            Files.write(path,old);WorldStore.save(path,world);
            check(Arrays.equals(old,Files.readAllBytes(dir.resolve("world.bin.bak"))),"original v1 backup not retained");
            check(java.nio.ByteBuffer.wrap(Files.readAllBytes(path)).getInt(4)==2,"migration did not write v2");
        }finally{try(var files=Files.list(dir)){for(var file:files.toList())Files.delete(file);}Files.delete(dir);}
    }
}
