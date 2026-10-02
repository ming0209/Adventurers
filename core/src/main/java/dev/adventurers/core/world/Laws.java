package dev.adventurers.core.world;

import dev.adventurers.core.api.Numbers;
import java.util.*;

public record Laws(Set<Domain> enabled, double gravity, double magicDensity) {
    public enum Domain { PHYSICS, CLIMATE, LIFE, MAGIC, TECHNOLOGY }
    public Laws {
        enabled = Set.copyOf(enabled);
        gravity = Numbers.clamp(gravity, .05, 10);
        magicDensity = Numbers.clamp(magicDensity, 0, 10);
    }
    public boolean allows(Domain domain) { return enabled.contains(domain); }
    /** Planet overrides may add laws, never switch off a parent law. */
    public Laws planet(Set<Domain> additions, double gravity, double magicDensity) {
        var all = EnumSet.noneOf(Domain.class);
        all.addAll(enabled);
        all.addAll(additions);
        return new Laws(all, gravity, magicDensity);
    }
    public static Laws overworld() { return new Laws(EnumSet.allOf(Domain.class), 1, 1); }
}
