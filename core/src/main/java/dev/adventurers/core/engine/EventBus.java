package dev.adventurers.core.engine;

import dev.adventurers.core.api.WorldEvent;
import java.util.*;
import java.util.function.Consumer;

/** Queued delivery prevents recursive event chains; subscriptions are never serialized. */
public final class EventBus {
    private final Map<String, List<Consumer<WorldEvent>>> listeners = new LinkedHashMap<>();
    private final ArrayDeque<WorldEvent> pending = new ArrayDeque<>();
    private final int limit;
    public EventBus(int limit) {
        if (limit < 1) throw new IllegalArgumentException();
        this.limit = limit;
    }
    public void subscribe(String type, Consumer<WorldEvent> listener) {
        listeners.computeIfAbsent(type, ignored -> new ArrayList<>()).add(listener);
    }
    public void publish(WorldEvent event) {
        if (pending.size() >= limit) throw new IllegalStateException("Event queue budget exceeded");
        pending.add(event);
    }
    public void flush() {
        int delivered = 0;
        while (!pending.isEmpty()) {
            if (++delivered > limit) throw new IllegalStateException("Cyclic event cascade");
            WorldEvent event = pending.removeFirst();
            for (var consumer : List.copyOf(listeners.getOrDefault(event.type(), List.of()))) consumer.accept(event);
            for (var consumer : List.copyOf(listeners.getOrDefault("*", List.of()))) consumer.accept(event);
        }
    }
}
