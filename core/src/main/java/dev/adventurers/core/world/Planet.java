package dev.adventurers.core.world;

import dev.adventurers.core.life.Genome;
import java.util.*;

/** A latitude/longitude sphere. Pole crossing reflects latitude and rotates longitude 180 degrees. */
public final class Planet {
    private final int columns;
    private final int rows;
    private final List<Region> regions;
    public Planet(int columns, int rows, List<Region> regions) {
        if (columns < 4 || columns % 2 != 0 || rows < 2 || columns * rows != regions.size()) throw new IllegalArgumentException();
        this.columns = columns; this.rows = rows; this.regions = List.copyOf(regions);
        for (int i = 0; i < regions.size(); i++) if (regions.get(i).view().id() != i) throw new IllegalArgumentException("Region index mismatch");
    }
    public int columns() { return columns; }
    public int rows() { return rows; }
    public List<Region> regions() { return regions; }
    public Region region(int id) { return regions.get(id); }
    public int index(int x, int y) {
        while (y < 0 || y >= rows) {
            if (y < 0) { y = -y - 1; x += columns / 2; }
            if (y >= rows) { y = 2 * rows - y - 1; x += columns / 2; }
        }
        return y * columns + Math.floorMod(x, columns);
    }
    public List<Region.View> neighbors(int id) {
        int x = id % columns, y = id / columns;
        return List.of(region(index(x - 1, y)).view(), region(index(x + 1, y)).view(),
                region(index(x, y - 1)).view(), region(index(x, y + 1)).view());
    }
    private record Plate(double x, double y, double z, double buoyancy, double drift) {}
    public static Planet genesis(long seed, int columns, int rows) {
        var random = new SplittableRandom(seed);
        var plates = new ArrayList<Plate>();
        for (int i = 0; i < 12; i++) {
            double y = random.nextDouble(-1, 1), a = random.nextDouble(-Math.PI, Math.PI), r = Math.sqrt(1 - y * y);
            plates.add(new Plate(Math.cos(a) * r, y, Math.sin(a) * r, random.nextDouble(-1, 1), random.nextDouble(-1, 1)));
        }
        var regions = new ArrayList<Region>();
        for (int row = 0; row < rows; row++) for (int col = 0; col < columns; col++) {
            double lat = -Math.PI / 2 + Math.PI * (row + .5) / rows, lon = -Math.PI + 2 * Math.PI * (col + .5) / columns;
            double x = Math.cos(lat) * Math.cos(lon), y = Math.sin(lat), z = Math.cos(lat) * Math.sin(lon);
            var sorted = plates.stream().sorted(Comparator.comparingDouble(p -> -(p.x * x + p.y * y + p.z * z))).toList();
            Plate a = sorted.get(0), b = sorted.get(1);
            double edge = Math.abs((a.x - b.x) * x + (a.y - b.y) * y + (a.z - b.z) * z);
            double convergence = Math.max(0, a.drift - b.drift);
            double elevation = a.buoyancy * 1200 + Math.exp(-edge * 20) * convergence * 1800;
            double temperature = 28 - 45 * Math.abs(Math.sin(lat)) - Math.max(0, elevation) * .006;
            regions.add(new Region(new Region.View(regions.size(), lat, lon, elevation, temperature,
                    elevation < 0 ? .8 : .45, 1, elevation < 0 ? 1 : .4, 0, .5 + Math.abs(a.buoyancy),
                    Math.min(1, Math.abs(a.buoyancy) * .5 + convergence * .2), Genome.primitive(temperature), 0)));
        }
        return new Planet(columns, rows, regions);
    }
}
