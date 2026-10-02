package dev.adventurers.core.engine;

import dev.adventurers.core.api.*;
import dev.adventurers.core.world.WorldModel;
import java.util.SplittableRandom;

public record LoopContext(WorldModel world, EventBus events, long tick, long elapsed, String system) {
    public SplittableRandom random(long entity) {
        return new SplittableRandom(Numbers.mix(world.seed() ^ Numbers.mix(tick) ^ Numbers.mix(entity) ^ system.hashCode()));
    }
    public double days() { return (double) elapsed / WorldTime.TICKS_PER_DAY; }
    public void emit(String type, String subject, String detail, double importance) {
        events.publish(new WorldEvent(tick, type, subject, detail, importance));
    }
}
