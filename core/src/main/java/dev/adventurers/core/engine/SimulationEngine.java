package dev.adventurers.core.engine;

import dev.adventurers.core.world.WorldModel;
import java.util.*;

/** Visits due systems, not all entities every Minecraft tick. Ties follow registration order. */
public final class SimulationEngine {
    private record Scheduled(LoopSystem<?, ?, ?> system, long due, int order) {}
    private final PriorityQueue<Scheduled> queue = new PriorityQueue<>(Comparator.comparingLong(Scheduled::due).thenComparingInt(Scheduled::order));
    private final Set<String> ids = new HashSet<>();
    private final WorldModel world;
    private final EventBus events;
    private long requestedTick;

    public SimulationEngine(WorldModel world, EventBus events) {
        this.world = world;
        this.events = events;
        requestedTick = world.tick();
    }
    public void register(LoopSystem<?, ?, ?> system) {
        var definition = system.definition();
        if (!ids.add(definition.id())) throw new IllegalArgumentException("Duplicate system " + definition.id());
        long due = Math.multiplyExact(Math.addExact(world.tick() / definition.period(), 1), definition.period());
        queue.add(new Scheduled(system, due, ids.size()));
    }
    /** A bounded catch-up operation. False means more work remains, never silently drops time. */
    public boolean advanceTo(long target, int budget) {
        if (target < world.tick() || target < requestedTick || budget < 1) throw new IllegalArgumentException("Invalid clock/budget");
        requestedTick = target;
        int work = 0;
        while (!queue.isEmpty() && queue.peek().due <= target && work < budget) {
            // Finish an entire timestamp so a snapshot can reconstruct every next due time.
            // A timestamp may exceed the work budget by at most the registered system count.
            long timestamp = queue.peek().due;
            do {
                var due = queue.remove();
                world.setTick(due.due);
                run(due.system, due.due);
                events.flush();
                queue.add(new Scheduled(due.system, Math.addExact(due.due, due.system.definition().period()), due.order));
                work++;
            } while (!queue.isEmpty() && queue.peek().due == timestamp);
        }
        if (!queue.isEmpty() && queue.peek().due <= target) return false;
        world.setTick(target);
        return true;
    }
    private <P, N, D> void run(LoopSystem<P, N, D> system, long tick) {
        var context = new LoopContext(world, events, tick, system.definition().period(), system.definition().id());
        P perception = system.perceive(context);
        N needs = system.evaluate(context, perception);
        D decision = system.decide(context, perception, needs);
        system.execute(context, decision);
        system.feedback(context, decision);
        system.evolve(context, decision);
    }
    public void advanceFully(long target) {
        while (!advanceTo(target, 4096)) { /* caller explicitly opted into offline catch-up */ }
    }
    public long requestedTick() { return requestedTick; }
}
