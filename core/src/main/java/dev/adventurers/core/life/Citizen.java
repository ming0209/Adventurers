package dev.adventurers.core.life;

import dev.adventurers.core.api.Numbers;
import java.util.*;

public final class Citizen {
    /** Activities are capabilities, not assigned professions. Repetition yields a social role. */
    public enum Activity { GATHER, BUILD, STUDY, HEAL, GUARD, REST, SOCIALIZE }
    private final long id;
    private final long city;
    private final Genome genome;
    private double ageDays;
    private double health = 1;
    private double hunger = .2;
    private double stress = .1;
    private Activity activity = Activity.GATHER;
    private int repetitions;
    private final EnumMap<Activity, Double> skills = new EnumMap<>(Activity.class);
    private final Memory memory = new Memory(16);

    public Citizen(long id, long city, Genome genome, double ageDays) {
        this.id = id; this.city = city; this.genome = genome; this.ageDays = Numbers.nonNegative(ageDays);
    }
    public long id() { return id; }
    public long city() { return city; }
    public Genome genome() { return genome; }
    public double ageDays() { return ageDays; }
    public double health() { return health; }
    public double hunger() { return hunger; }
    public double stress() { return stress; }
    public Activity activity() { return activity; }
    public int repetitions() { return repetitions; }
    public boolean alive() { return health > 0; }
    public void injure(double damage) { health = Numbers.unit(health - Numbers.nonNegative(damage)); }
    public String role() { return repetitions >= 6 ? activity.name().toLowerCase(Locale.ROOT) : "unsettled"; }
    public double skill(Activity activity) { return skills.getOrDefault(activity, .05); }
    public Map<Activity, Double> skills() { return Map.copyOf(skills); }
    public Memory memory() { return memory; }
    public void choose(Activity next) {
        repetitions = next == activity ? repetitions + 1 : 1;
        activity = next;
    }
    public void live(double days, double foodRatio, double disease, double shelter) {
        ageDays += days;
        hunger = Numbers.unit(hunger + (.55 - foodRatio) * days * .25);
        stress = Numbers.unit(stress + (hunger + disease - shelter * .5 - .2) * days * .1);
        double senescence = Math.max(0, ageDays / 24 - 55) * .0008;
        health = Numbers.unit(health + (.015 * foodRatio - hunger * .04 - disease * .035 - senescence) * days);
        skills.put(activity, Numbers.unit(skill(activity) + .003 * days * (.5 + genome.cognition())));
    }
    public void restore(double health, double hunger, double stress, Activity activity, int repetitions, Map<Activity, Double> skills) {
        this.health = Numbers.unit(health); this.hunger = Numbers.unit(hunger); this.stress = Numbers.unit(stress);
        this.activity = Objects.requireNonNull(activity);
        if (repetitions < 0) throw new IllegalArgumentException();
        this.repetitions = repetitions;
        skills.forEach((key, value) -> this.skills.put(key, Numbers.unit(value)));
    }
}
