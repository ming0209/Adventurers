package dev.adventurers.core.world;

import dev.adventurers.core.api.Numbers;
import java.util.*;

public final class Stockpile {
    private final EnumMap<Resource, Double> amounts = new EnumMap<>(Resource.class);
    public double get(Resource resource) { return amounts.getOrDefault(resource, 0.0); }
    public void add(Resource resource, double amount) {
        Numbers.nonNegative(amount);
        double total = get(resource) + amount;
        Numbers.nonNegative(total);
        amounts.put(resource, total);
    }
    public double take(Resource resource, double requested) {
        Numbers.nonNegative(requested);
        double taken = Math.min(requested, get(resource));
        amounts.put(resource, get(resource) - taken);
        return taken;
    }
    /** Validate the whole recipe before subtracting anything. */
    public boolean consume(Map<Resource, Double> cost) {
        cost.values().forEach(Numbers::nonNegative);
        if (cost.entrySet().stream().anyMatch(e -> get(e.getKey()) < e.getValue())) return false;
        cost.forEach(this::take);
        return true;
    }
    public Map<Resource, Double> view() { return Collections.unmodifiableMap(new EnumMap<>(amounts)); }
}
