package dev.adventurers.core.player;

import dev.adventurers.core.api.Numbers;
import dev.adventurers.core.magic.*;
import java.util.*;

public final class PlayerProfile {
    public enum Origin { BORN, SUMMONED, TRANSMIGRATED }
    private final UUID id;
    private long city;
    private Origin origin;
    private int incarnation;
    private double mana = 30, mentalCapacity = 60, consciousness = 10, reputation;
    private long lastMeditation = -200, lastCast = -40;
    private boolean banner = true;
    private final Set<Element> knowledge = EnumSet.of(Element.SPACE);
    private final Map<String, Spell> spells = new LinkedHashMap<>();
    public PlayerProfile(UUID id, long city, Origin origin, int incarnation) {
        this.id = id; this.city = city; this.origin = origin; this.incarnation = incarnation;
    }
    public UUID id() { return id; }
    public long city() { return city; }
    public Origin origin() { return origin; }
    public int incarnation() { return incarnation; }
    public double mana() { return mana; }
    public double mentalCapacity() { return mentalCapacity; }
    public double consciousness() { return consciousness; }
    public double reputation() { return reputation; }
    public long lastMeditation() { return lastMeditation; }
    public long lastCast() { return lastCast; }
    public boolean banner() { return banner; }
    public void banner(boolean value) { banner = value; }
    public Set<Element> knowledge() { return Set.copyOf(knowledge); }
    public Map<String, Spell> spells() { return Collections.unmodifiableMap(spells); }
    public void learn(Element element) { knowledge.add(element); }
    public void remember(Spell spell) {
        if (!spells.containsKey(spell.name()) && spells.size() >= 32) throw new IllegalStateException("法术记忆已满（32）");
        spells.put(spell.name(), spell);
    }
    public void meditate(long tick, double ambientMana) {
        if (tick - lastMeditation < 200) throw new IllegalStateException("冥想需要间隔十秒");
        lastMeditation = tick;
        mentalCapacity = Math.min(300, mentalCapacity + .2);
        consciousness = Math.min(100, consciousness + .1);
        mana = Math.min(mentalCapacity, mana + Math.min(10, Numbers.nonNegative(ambientMana) * 5));
    }
    public void cast(long tick, double cost) {
        Numbers.nonNegative(cost);
        if (tick - lastCast < 40) throw new IllegalStateException("尚未完成引导");
        if (mana < cost) throw new IllegalStateException("魔力不足，请先冥想");
        mana -= cost; lastCast = tick;
    }
    public void reward(double amount) { reputation += Numbers.nonNegative(amount); }
    public void penalize(double amount) { reputation -= Numbers.nonNegative(amount); }
    public void restore(double mana, double mentalCapacity, double consciousness, double reputation,
                        long lastMeditation, long lastCast, boolean banner, Set<Element> knowledge) {
        this.mentalCapacity = Numbers.clamp(mentalCapacity, 1, 300); this.mana = Numbers.clamp(mana, 0, this.mentalCapacity);
        this.consciousness = Numbers.clamp(consciousness, 0, 100); this.reputation = Numbers.clamp(reputation, -1e9, 1e9);
        this.lastMeditation = lastMeditation; this.lastCast = lastCast; this.banner = banner;
        this.knowledge.clear(); this.knowledge.addAll(knowledge);
    }
}
