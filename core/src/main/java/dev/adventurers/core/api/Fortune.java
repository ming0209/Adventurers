package dev.adventurers.core.api;

/** A distribution of resilience, not an experience level or a single luck counter. */
public record Fortune(double cohesion, double reserves, double adaptability) {
    public Fortune {
        cohesion = Numbers.unit(cohesion);
        reserves = Numbers.unit(reserves);
        adaptability = Numbers.unit(adaptability);
    }
    public double stability() { return .4 * cohesion + .35 * reserves + .25 * adaptability; }
    public Fortune shift(double internal, double external) {
        return new Fortune(cohesion - internal, reserves - external, adaptability + (internal + external) * .05);
    }
    public static Fortune healthy() { return new Fortune(.7, .7, .5); }
}
