package dev.adventurers.core.world;

import dev.adventurers.core.api.Numbers;
import dev.adventurers.core.life.Genome;

public final class Region {
    public record View(int id, double latitude, double longitude, double elevation, double temperature,
                       double moisture, double pressure, double water, double biomass, double mana,
                       double ore, Genome genome, int generation) {}
    private View state;
    public Region(View state) { update(state); }
    public View view() { return state; }
    public void update(View next) {
        if (state != null && state.id() != next.id()) throw new IllegalArgumentException();
        state = new View(next.id(), Numbers.clamp(next.latitude(), -Math.PI / 2, Math.PI / 2),
                Numbers.clamp(next.longitude(), -Math.PI, Math.PI), Numbers.clamp(next.elevation(), -4000, 6000),
                Numbers.clamp(next.temperature(), -80, 80), Numbers.unit(next.moisture()),
                Numbers.clamp(next.pressure(), .1, 2), Numbers.nonNegative(next.water()),
                Numbers.unit(next.biomass()), Numbers.nonNegative(next.mana()), Numbers.unit(next.ore()),
                next.genome(), Math.max(0, next.generation()));
    }
    public boolean habitable() { return state.elevation() >= 0 && state.biomass() > .2 && state.water() > .1; }
}
