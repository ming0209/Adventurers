package dev.adventurers.core.life;

import dev.adventurers.core.api.*;
import java.util.*;

public final class Memory {
    public record Entry(long tick, String event, double importance) {
        public Entry { importance = Numbers.unit(importance); }
        double weight(long now) {
            return importance >= .95 ? importance : importance * Math.exp(-(now - tick) / (WorldTime.TICKS_PER_DAY * 12.0));
        }
    }
    private final int capacity;
    private final List<Entry> entries = new ArrayList<>();
    public Memory(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException();
        this.capacity = capacity;
    }
    public void remember(Entry entry, long now) {
        entries.removeIf(e -> e.weight(now) < .03);
        entries.add(entry);
        if (entries.size() > capacity) entries.remove(entries.stream().min(Comparator.comparingDouble(e -> e.weight(now))).orElseThrow());
    }
    public List<Entry> entries() { return List.copyOf(entries); }
}
