package dev.adventurers.core.api;

public final class Numbers {
    private Numbers() {}

    public static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite number");
        return Math.max(min, Math.min(max, value));
    }

    public static double unit(double value) { return clamp(value, 0, 1); }

    public static double nonNegative(double value) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Expected finite nonnegative number");
        return value;
    }

    /** Stateless SplitMix64: results do not depend on iteration order or save/reload. */
    public static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
