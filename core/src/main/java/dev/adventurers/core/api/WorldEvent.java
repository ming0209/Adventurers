package dev.adventurers.core.api;

public record WorldEvent(long tick, String type, String subject, String detail, double importance) {
    public WorldEvent {
        if (tick < 0 || type == null || subject == null || detail == null) throw new IllegalArgumentException();
        importance = Numbers.unit(importance);
    }
}
