package dev.adventurers.core.magic;

/** The supplied documents name 25 distinct elements despite their "26" heading. */
public enum Element {
    WATER("水", Tier.BASE, Polarity.MOON), FIRE("火", Tier.BASE, Polarity.SUN),
    LIGHT("光", Tier.BASE, Polarity.SUN), DARK("暗", Tier.BASE, Polarity.MOON),
    GRASS("草", Tier.BASE, Polarity.SUN), EARTH("土", Tier.BASE, Polarity.MOON),
    WIND("风", Tier.BASE, Polarity.MOON), ELECTRICITY("电", Tier.BASE, Polarity.SUN),
    THUNDER("雷", Tier.COMPOSITE, Polarity.SUN), HOLY_LIGHT("圣光", Tier.COMPOSITE, Polarity.SUN),
    SPIRIT("灵", Tier.COMPOSITE, Polarity.SUN), WOOD("木", Tier.COMPOSITE, Polarity.MOON),
    WITCHCRAFT("巫术", Tier.COMPOSITE, Polarity.MOON), ICE("冰", Tier.COMPOSITE, Polarity.MOON),
    POISON("毒", Tier.COMPOSITE, Polarity.MOON), STORM("风暴", Tier.COMPOSITE, Polarity.SUN),
    LIFE("生命", Tier.DIVINE, Polarity.SUN), NATURE("自然", Tier.DIVINE, Polarity.SUN),
    HOLINESS("神圣", Tier.DIVINE, Polarity.SUN), MYSTERY("神秘", Tier.DIVINE, Polarity.MOON),
    DESTRUCTION("毁灭", Tier.DIVINE, Polarity.SUN), CALAMITY("灾异", Tier.DIVINE, Polarity.MOON),
    WITHER("凋零", Tier.DIVINE, Polarity.MOON), NECROMANCY("死灵", Tier.DIVINE, Polarity.MOON),
    SPACE("空间", Tier.PURE, Polarity.CHAOS);
    public enum Tier { BASE, COMPOSITE, DIVINE, PURE }
    public enum Polarity { SUN, MOON, CHAOS }
    public enum State { DORMANT, ACTIVE, INACTIVE }
    private final String label;
    private final Tier tier;
    private final Polarity polarity;
    Element(String label, Tier tier, Polarity polarity) { this.label = label; this.tier = tier; this.polarity = polarity; }
    public String label() { return label; }
    public Tier tier() { return tier; }
    public Polarity polarity() { return polarity; }
    public boolean opposes(Element other) {
        return polarity != Polarity.CHAOS && other.polarity != Polarity.CHAOS && polarity != other.polarity;
    }
}
