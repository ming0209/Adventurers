package dev.adventurers.core.civilization;

import dev.adventurers.core.api.Fortune;
import java.util.List;

/** Data aggregate only. Policies are decided by power centers, never by this container. */
public record Civilization(long id, String name, long capital, List<Long> cities, Fortune fortune) {
    public Civilization { cities = List.copyOf(cities); }
}
