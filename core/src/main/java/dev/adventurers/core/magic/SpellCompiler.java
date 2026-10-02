package dev.adventurers.core.magic;

import dev.adventurers.core.api.Numbers;
import dev.adventurers.core.world.Laws;
import java.util.*;

/** Shared deterministic validator for player designs and NPC experiments. Invalid programs never consume mana. */
public final class SpellCompiler {
    public record Result(List<String> errors, double cost, double strength, double stability, double volatility) {
        public Result { errors = List.copyOf(errors); }
        public boolean valid() { return errors.isEmpty(); }
    }
    public Result compile(Spell spell, Set<Element> knowledge, Laws laws) {
        var errors = new ArrayList<String>();
        var circle = spell.circle();
        if (!laws.allows(Laws.Domain.MAGIC)) errors.add("此世界的法则不允许魔法");
        if (!circle.eye()) errors.add("缺少眼：无法汲取能量");
        if (!circle.core()) errors.add("缺少核：无法释放能量");
        if (!circle.ring() && !circle.skeleton()) errors.add("缺少阵轮或阵骨");
        if (circle.conductivity() < 0) errors.add("材料不能传导魔力");
        var byLayer = new HashMap<Integer, Set<Element>>();
        int sun = 0, moon = 0, chaos = 0, active = 0;
        boolean reversed = spell.runes().stream().anyMatch(r -> r.kind() == Spell.Kind.REVERSED);
        for (var rune : spell.runes()) {
            if (!knowledge.contains(rune.element())) errors.add("未掌握元素：" + rune.element().label());
            if (reversed && rune.kind() != Spell.Kind.REVERSED) errors.add("斥符不可接入普通符文序列");
            if (rune.kind() == Spell.Kind.SOUL || rune.kind() == Spell.Kind.CURSE || rune.kind() == Spell.Kind.SIGNATURE)
                errors.add("此符文需要尚未实现的灵魂或材料绑定");
            if (rune.kind() == Spell.Kind.EMPTY) continue;
            var elements = byLayer.computeIfAbsent(rune.layer(), key -> EnumSet.noneOf(Element.class));
            if (elements.stream().anyMatch(rune.element()::opposes)) errors.add("同层日月元素相斥");
            elements.add(rune.element());
            switch (rune.polarity()) { case SUN -> sun++; case MOON -> moon++; case CHAOS -> chaos++; }
            active++;
        }
        if (chaos > Math.max(sun, moon)) active -= chaos;
        else if (sun > moon) sun += chaos;
        else if (moon > sun) moon += chaos;
        if (active == 0) errors.add("空符序列没有效果");
        double cost = Math.max(1, active * 8 + (sun - moon) * 2) * (1 - circle.affinity() / 18);
        if (cost > circle.capacity()) errors.add("能耗超过载能");
        double stability = Numbers.unit(circle.quality() * (circle.ring() ? 1 : .5) - spell.runes().size() * .005);
        double volatility = Numbers.unit(1 - stability + Math.max(0, cost - circle.capacity()) / Math.max(1, circle.capacity()));
        if (stability < .25) errors.add("结构不稳定");
        double strength = Math.max(0, active * 3 + sun - moon + (circle.capacity() - cost) * .025);
        return new Result(errors.stream().distinct().toList(), cost, strength, stability, volatility);
    }
}
