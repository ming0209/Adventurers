package dev.adventurers.core.life;

import dev.adventurers.core.api.Numbers;
import java.util.SplittableRandom;

/** Continuous heritable traits shared by plants and animals; no fixed species pool. */
public record Genome(double warmth, double waterNeed, double size, double mobility,
                     double cognition, double sociality, double defense, double manaAffinity) {
    public Genome {
        warmth = Numbers.clamp(warmth, -40, 50);
        waterNeed = Numbers.unit(waterNeed);
        size = Numbers.clamp(size, .05, 10);
        mobility = Numbers.unit(mobility);
        cognition = Numbers.unit(cognition);
        sociality = Numbers.unit(sociality);
        defense = Numbers.unit(defense);
        manaAffinity = Numbers.unit(manaAffinity);
    }
    public double fitness(double temperature, double moisture, double mana) {
        return Numbers.unit(1 - Math.abs(warmth - temperature) / 65
                - Math.max(0, waterNeed - moisture) * .7 + Math.min(mana, 1) * manaAffinity * .08 - size * .012);
    }
    public Genome offspring(Genome other, SplittableRandom random) {
        return new Genome((warmth + other.warmth) / 2 + random.nextDouble(-3, 3),
                (waterNeed + other.waterNeed) / 2 + random.nextDouble(-.08, .08),
                (size + other.size) / 2 * random.nextDouble(.9, 1.1),
                (mobility + other.mobility) / 2 + random.nextDouble(-.08, .08),
                (cognition + other.cognition) / 2 + random.nextDouble(-.06, .08),
                (sociality + other.sociality) / 2 + random.nextDouble(-.07, .07),
                (defense + other.defense) / 2 + random.nextDouble(-.08, .08),
                (manaAffinity + other.manaAffinity) / 2 + random.nextDouble(-.08, .08));
    }
    public static Genome primitive(double temperature) { return new Genome(temperature, .35, .2, .1, .08, .15, .1, .1); }
}
