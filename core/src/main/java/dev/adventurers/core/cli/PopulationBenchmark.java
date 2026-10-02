package dev.adventurers.core.cli;

import dev.adventurers.core.life.Citizen;
import dev.adventurers.core.persistence.WorldCodec;
import dev.adventurers.core.world.*;

/** Measures a cold abstract population, not 100,000 rendered Minecraft entities. */
public final class PopulationBenchmark {
    public static void main(String[] args) throws Exception {
        var world=new WorldModel(42,Planet.genesis(42,24,12),Laws.overworld(),1,100000);
        var city=world.foundCity(world.planet().regions().stream().filter(r->r.view().elevation()>0).findFirst().orElseThrow());
        var genome=city.citizens().get(0).genome();
        while(city.citizens().size()<100000)city.addCitizen(new Citizen(world.allocateId(),city.id(),genome,20*24));
        city.stocks().add(Resource.FOOD,500000);
        var simulation=new Simulation(world);
        long start=System.nanoTime();simulation.advanceDays(1);long elapsed=System.nanoTime()-start;
        byte[] snapshot=WorldCodec.encode(world);var decoded=WorldCodec.decode(snapshot);
        if(decoded.population()!=100000)throw new AssertionError("Population lost");
        System.out.printf("COLD NPCs=%d, one simulated day=%.1f ms, snapshot=%.2f MiB; no entity rendering benchmark%n",world.population(),elapsed/1e6,snapshot.length/1048576.0);
    }
}
