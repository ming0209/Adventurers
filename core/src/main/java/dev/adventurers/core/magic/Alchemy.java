package dev.adventurers.core.magic;

import dev.adventurers.core.api.Numbers;
import java.util.*;

public final class Alchemy {
    public enum Layer { SOURCE, PRE, POST }
    public enum Method { DRY, GRIND, FERMENT, DISTILL, SEPARATE }
    public record Trait(Element element, Layer layer, int level, boolean floating) {
        public Trait { if (level < 1 || level > 99) throw new IllegalArgumentException(); }
    }
    public record Material(String name, Element primary, List<Trait> traits, List<Method> history,
                           boolean separated, double conductivity, double affinity) {
        public Material {
            traits = List.copyOf(traits); history = List.copyOf(history);
            conductivity = Numbers.clamp(conductivity, -9, 9); affinity = Numbers.clamp(affinity, -9, 9);
        }
    }
    public Material process(Material input, Method method, Element reagent) {
        if (input.separated()) throw new IllegalArgumentException("离析后不可继续加工");
        if (input.history().contains(method)) throw new IllegalArgumentException("加工分支不可重复");
        if (input.history().isEmpty() && !input.primary().opposes(reagent)) throw new IllegalArgumentException("第一步需使用相斥元素");
        var traits = new ArrayList<Trait>();
        for (var trait : input.traits()) {
            if (trait.layer() == Layer.SOURCE || !trait.floating()) { traits.add(trait); continue; }
            if (!trait.element().opposes(reagent)) {
                Element element = trait.element().polarity() == reagent.polarity() ? reagent : trait.element();
                traits.add(new Trait(element, trait.layer(), trait.level(), true));
            }
        }
        traits.add(new Trait(reagent, Layer.PRE, 1, true));
        var history = new ArrayList<>(input.history()); history.add(method);
        return new Material(input.name(), input.primary(), traits, history, method == Method.SEPARATE,
                input.conductivity(), input.affinity());
    }
}
