package dev.adventurers.core.civilization;

import dev.adventurers.core.api.*;
import dev.adventurers.core.life.Citizen;
import dev.adventurers.core.world.*;
import java.util.*;

public final class City {
    public record Power(String name, Citizen.Activity interest, double weight) {
        public Power { weight = Numbers.nonNegative(weight); }
    }
    public enum Era { STONE, BRONZE, IRON, GUNPOWDER, STEAM, ELECTRIC, INFORMATION }
    private final long id;
    private final long civilization;
    private final int region;
    private final String name;
    private final int x, z;
    private final Stockpile stocks = new Stockpile();
    private final List<Citizen> citizens = new ArrayList<>();
    private final Set<Long> citizenIds = new HashSet<>();
    private final List<Building> buildings = new ArrayList<>();
    private final List<Power> powers = new ArrayList<>();
    private final EnumMap<Resource, Double> prices = new EnumMap<>(Resource.class);
    private Fortune fortune = Fortune.healthy();
    private double disease, technology, magic;

    public City(long id, long civilization, int region, String name, int x, int z) {
        this.id = id; this.civilization = civilization; this.region = region; this.name = Objects.requireNonNull(name); this.x = x; this.z = z;
    }
    public long id() { return id; }
    public long civilization() { return civilization; }
    public int region() { return region; }
    public String name() { return name; }
    public int x() { return x; }
    public int z() { return z; }
    public Stockpile stocks() { return stocks; }
    public List<Citizen> citizens() { return Collections.unmodifiableList(citizens); }
    public List<Building> buildings() { return Collections.unmodifiableList(buildings); }
    public List<Power> powers() { return List.copyOf(powers); }
    public Fortune fortune() { return fortune; }
    public double disease() { return disease; }
    public double technology() { return technology; }
    public double magic() { return magic; }
    public Era era() { return Era.values()[Math.min(Era.values().length - 1, (int) technology)]; }
    public int population() { return (int) citizens.stream().filter(Citizen::alive).count(); }
    public int housing() { return 8 + buildings.stream().mapToInt(Building::capacity).sum(); }
    public double foodDays() { return stocks.get(Resource.FOOD) / Math.max(1, population()); }
    public double price(Resource resource) { return prices.getOrDefault(resource, 1.0); }
    public Map<Resource, Double> prices() { return Map.copyOf(prices); }
    public void price(Resource resource, double price) { prices.put(resource, Numbers.clamp(price, .1, 100)); }
    public void addCitizen(Citizen citizen) {
        if (citizen.city() != id || !citizenIds.add(citizen.id())) throw new IllegalArgumentException();
        citizens.add(citizen);
    }
    public void pruneDeceased() {
        citizens.removeIf(citizen -> {
            if (citizen.alive()) return false;
            citizenIds.remove(citizen.id()); return true;
        });
    }
    public void addBuilding(Building building) {
        if (buildings.stream().anyMatch(b -> b.id() == building.id())) throw new IllegalArgumentException();
        buildings.add(building);
    }
    public void powers(List<Power> next) { powers.clear(); powers.addAll(next); }
    public void fortune(Fortune next) { fortune = Objects.requireNonNull(next); }
    public void disease(double next) { disease = Numbers.unit(next); }
    public void knowledge(double technology, double magic) {
        this.technology = Numbers.clamp(technology, 0, 7);
        this.magic = Numbers.clamp(magic, 0, 25);
    }
}
