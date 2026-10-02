package dev.adventurers.core.world;

import java.util.List;

public enum LoadingTier {
    HOT, WARM, COLD;
    public record Observer(double x, double z) {}
    public static LoadingTier at(double x, double z, List<Observer> observers) {
        double closest = Double.POSITIVE_INFINITY;
        for (var observer : observers) closest = Math.min(closest, Math.hypot(x - observer.x, z - observer.z));
        return closest <= 128 ? HOT : closest <= 384 ? WARM : COLD;
    }
}
