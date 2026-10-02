package dev.adventurers.core.civilization;

import dev.adventurers.core.api.Numbers;
import dev.adventurers.core.life.Assembly;
import dev.adventurers.core.world.*;
import java.util.*;

public final class Building {
    public enum Stage { FOUNDATION, FRAME, WALLS, ROOF, OPENINGS, INTERIOR, COMPLETE, RUIN }
    private final long id;
    private final int width, depth;
    private Stage stage = Stage.FOUNDATION;
    private double progress;
    private double integrity = 1;
    public Building(long id, int width, int depth) {
        if (width < 3 || width > 15 || depth < 3 || depth > 15) throw new IllegalArgumentException();
        this.id = id; this.width = width; this.depth = depth;
    }
    public long id() { return id; }
    public int width() { return width; }
    public int depth() { return depth; }
    public Stage stage() { return stage; }
    public double progress() { return progress; }
    public double integrity() { return integrity; }
    public int capacity() { return stage == Stage.COMPLETE ? Math.max(1, width * depth / 4) : 0; }
    public void work(double labor, Stockpile stock) {
        Numbers.nonNegative(labor);
        if (stage.ordinal() >= Stage.COMPLETE.ordinal() || labor <= 0) return;
        double needed = width * depth * .12;
        double applied = Math.min(labor, needed - progress);
        Resource resource = stage == Stage.FOUNDATION ? Resource.STONE : Resource.WOOD;
        if (!stock.consume(Map.of(resource, applied * .4))) return;
        progress += applied;
        if (progress + 1e-9 >= needed) { stage = Stage.values()[stage.ordinal() + 1]; progress = 0; }
    }
    public void decay(double amount) {
        integrity = Numbers.unit(integrity - Numbers.nonNegative(amount));
        if (integrity == 0) stage = Stage.RUIN;
    }
    public Assembly assembly() {
        var parts = new ArrayList<Assembly.Part>();
        for (int i = 0; i <= Math.min(stage.ordinal(), Stage.INTERIOR.ordinal()); i++) {
            parts.add(new Assembly.Part(Stage.values()[i].name(), i == 0 ? null : Stage.FOUNDATION.name(),
                    i == 0 ? "stone" : "wood", integrity, i == 0));
        }
        return new Assembly(parts);
    }
    public void restore(Stage stage, double progress, double integrity) {
        this.stage = Objects.requireNonNull(stage); this.progress = Numbers.nonNegative(progress); this.integrity = Numbers.unit(integrity);
    }
}
