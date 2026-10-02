package dev.adventurers.core.world;

/** Equirectangular chart of a sphere, not a torus. Crossing a pole turns longitude by 180 degrees. */
public final class PlanetCoordinates {
    public record Point(double x, double z, boolean reflected) {}
    private PlanetCoordinates() {}
    public static Point normalize(double x, double z, TerrainSettings settings) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) throw new IllegalArgumentException("Non-finite coordinate");
        double width = settings.circumference(), height = settings.poleDistance();
        double latitude = mod(z + height / 2, height * 2);
        boolean reflected = latitude >= height;
        if (reflected) { latitude = height * 2 - latitude; x += width / 2; }
        return new Point(mod(x + width / 2, width) - width / 2, latitude - height / 2, reflected);
    }
    public static double longitudeDistance(double a, double b, double circumference) {
        return mod(a - b + circumference / 2, circumference) - circumference / 2;
    }
    public static int region(double x, double z, TerrainSettings settings) {
        var p = normalize(x, z, settings);
        int col = Math.floorMod((int)Math.floor((p.x() + settings.circumference() / 2.0) / settings.regionSize()), TerrainSettings.COLUMNS);
        int row = Math.min(TerrainSettings.ROWS - 1, Math.max(0, (int)Math.floor((p.z() + settings.poleDistance() / 2.0) / settings.regionSize())));
        return row * TerrainSettings.COLUMNS + col;
    }
    private static double mod(double value, double modulus) { return value - Math.floor(value / modulus) * modulus; }
}
