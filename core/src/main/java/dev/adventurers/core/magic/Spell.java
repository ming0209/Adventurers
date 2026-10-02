package dev.adventurers.core.magic;

import dev.adventurers.core.api.Numbers;
import java.util.*;

public record Spell(String name, Effect effect, List<Rune> runes, Circle circle) {
    public enum Effect { RELEASE, SHIELD, HEAL, PURIFY }
    public enum Kind { MAGIC, EMPTY, REVERSED, APPENDED, SOUL, CURSE, SIGNATURE }
    public record Rune(Element element, Element.Polarity polarity, Kind kind, int layer) {
        public Rune {
            Objects.requireNonNull(element); Objects.requireNonNull(polarity); Objects.requireNonNull(kind);
            if (layer < 0 || layer > 8) throw new IllegalArgumentException("Layer must be 0..8");
        }
    }
    public record Circle(boolean eye, boolean core, boolean ring, boolean skeleton,
                         double capacity, double quality, double conductivity, double affinity) {
        public Circle {
            capacity = Numbers.nonNegative(capacity); quality = Numbers.unit(quality);
            conductivity = Numbers.clamp(conductivity, -9, 9); affinity = Numbers.clamp(affinity, -9, 9);
        }
    }
    public Spell {
        if (name == null || name.isBlank() || name.length() > 64) throw new IllegalArgumentException("Invalid spell name");
        Objects.requireNonNull(effect); Objects.requireNonNull(circle);
        runes = List.copyOf(runes);
        if (runes.isEmpty() || runes.size() > 64) throw new IllegalArgumentException("Spell needs 1..64 runes");
    }
    public static Spell simple(String name, Effect effect, Element element) {
        return new Spell(name, effect, List.of(new Rune(element, element.polarity(), Kind.MAGIC, 0)),
                new Circle(true, true, true, false, 40, .9, 3, 2));
    }
}
