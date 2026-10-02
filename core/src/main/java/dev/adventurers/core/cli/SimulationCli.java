package dev.adventurers.core.cli;

import dev.adventurers.core.civilization.City;
import dev.adventurers.core.persistence.WorldStore;
import dev.adventurers.core.world.*;
import java.nio.file.Path;

public final class SimulationCli {
    public static void main(String[] args) throws Exception {
        long seed = args.length > 0 ? Long.parseLong(args[0]) : 42;
        int days = args.length > 1 ? Integer.parseInt(args[1]) : 720;
        var simulation = new Simulation(WorldModel.create(seed));
        simulation.advanceDays(days);
        var world = simulation.world();
        System.out.printf("seed=%d day=%d cities=%d population=%d ready=%s%n", seed, world.tick() / 24000, world.cities().size(), world.population(), simulation.insertionReady(City.Era.STONE));
        for (var city : world.cities()) System.out.printf("%s: population=%d era=%s food=%.1fd housing=%d health=%.3f%n", city.name(),city.population(),city.era(),city.foodDays(),city.housing(),city.fortune().stability());
        if (args.length > 2) WorldStore.save(Path.of(args[2]), world);
    }
}
