package dev.adventurers.core.life;

import dev.adventurers.core.api.Numbers;
import java.util.*;

/** Shared parts model for bodies, buildings, equipment and rune structures. */
public record Assembly(List<Part> parts) {
    public record Part(String id, String parent, String material, double integrity, boolean vital) {
        public Part {
            if (id == null || id.isBlank() || material == null) throw new IllegalArgumentException();
            integrity = Numbers.unit(integrity);
        }
    }
    public Assembly {
        parts = List.copyOf(parts);
        var ids = new HashSet<String>();
        for (var part : parts) {
            if (!ids.add(part.id())) throw new IllegalArgumentException("Duplicate part " + part.id());
            if (part.parent() != null && !ids.contains(part.parent())) throw new IllegalArgumentException("Parent must precede child");
            if (part.id().equals(part.parent())) throw new IllegalArgumentException("Part cycle");
        }
    }
    public boolean viable() { return parts.stream().noneMatch(p -> p.vital() && p.integrity() <= 0); }
    public double integrity() { return parts.stream().mapToDouble(Part::integrity).average().orElse(0); }
    public Assembly damage(String id, double amount) {
        Numbers.nonNegative(amount);
        if (parts.stream().noneMatch(p -> p.id().equals(id))) throw new IllegalArgumentException("Unknown part");
        return new Assembly(parts.stream().map(p -> p.id().equals(id)
                ? new Part(p.id(), p.parent(), p.material(), p.integrity() - amount, p.vital()) : p).toList());
    }
}
