package dev.adventurers.core.systems;

import dev.adventurers.core.api.*;
import dev.adventurers.core.civilization.*;
import dev.adventurers.core.engine.*;
import dev.adventurers.core.life.*;
import dev.adventurers.core.player.Quest;
import dev.adventurers.core.world.*;
import java.util.*;
import static dev.adventurers.core.life.Citizen.Activity.*;

public final class SocietySystems {
    private SocietySystems() {}
    public static List<LoopSystem<?, ?, ?>> create() {
        return List.of(new People(), new Production(), new Disease(), new Construction(), new Politics(), new Market(), new Knowledge(), new Quests(), new Aggregation());
    }
    private abstract static class CitySystem extends DomainSystem<City> {
        CitySystem(String id, long period, String components, String inputs, String needs, String outputs) {
            super(id, period, components, inputs, needs, outputs);
        }
        public List<City> perceive(LoopContext c) { return List.copyOf(c.world().cities()); }
        protected double demand(LoopContext c, City city) { return city.population() > 0 ? 1 : 0; }
    }
    private static final class People extends CitySystem {
        People() { super("personal", WorldTime.TICKS_PER_DAY, "body,memory,skills", "food,shelter,health", "survival,safety,social,development", "activity,birth,death"); }
        protected void act(LoopContext c, City city, double urgency) {
            int population = city.population();
            double ratio = city.stocks().take(Resource.FOOD, population) / Math.max(1, population);
            boolean hungry = city.foodDays() < 3, crowded = population > city.housing();
            var random = c.random(city.id());
            for (var person : city.citizens()) {
                if (!person.alive()) continue;
                long selector = Math.floorMod(Numbers.mix(person.id()), 100);
                var activity = person.health() < .35 ? REST
                        : hungry && selector < 70 ? GATHER
                        : city.disease() > .15 && selector > 85 ? HEAL
                        : crowded && selector >= 50 && selector < 80 ? BUILD
                        : selector < 45 ? GATHER : selector < 65 ? BUILD : selector < 85 ? STUDY : selector < 92 ? GUARD : SOCIALIZE;
                person.choose(activity);
                person.live(c.days(), ratio, city.disease(), Math.min(1, (double) city.housing() / population));
                if (!person.alive()) c.emit("person.died", Long.toString(person.id()), city.name() + "失去一位成员", .65);
            }
            // A compatible adult pair, food reserves and housing drive reproduction.
            var adults = city.citizens().stream().filter(p -> p.alive() && p.ageDays() >= 16 * 24 && p.ageDays() < 42 * 24 && p.health() > .5).toList();
            if (adults.size() >= 2 && city.foodDays() > 2 && city.housing() > population
                    && c.world().population() < c.world().populationLimit() && random.nextDouble() < adults.size() * .004) {
                var a = adults.get(random.nextInt(adults.size()));
                var b = adults.get((adults.indexOf(a) + 1) % adults.size());
                city.addCitizen(new Citizen(c.world().allocateId(), city.id(), a.genome().offspring(b.genome(), random), 0));
                c.emit("person.born", Long.toString(city.id()), city.name() + "迎来新生命", .6);
            }
            city.pruneDeceased();
        }
        protected void respond(LoopContext c, City city) {
            double health = city.citizens().stream().filter(Citizen::alive).mapToDouble(Citizen::health).average().orElse(0);
            double skills = city.citizens().stream().filter(Citizen::alive).mapToDouble(p -> p.skill(p.activity())).average().orElse(0);
            city.fortune(new Fortune(health * (1 - city.disease()), Math.min(1, city.foodDays() / 5), skills));
            c.emit("city.needs", Long.toString(city.id()), "food=" + Math.round(city.foodDays()) + ",housing=" + city.housing(), .05);
        }
    }
    private static final class Production extends CitySystem {
        Production() { super("production", WorldTime.TICKS_PER_DAY, "labor,land,resource_pool", "activities,biomass", "supplies", "food,materials,mana"); }
        protected void act(LoopContext c, City city, double urgency) {
            var region = c.world().planet().region(city.region());
            var v = region.view();
            double harvested = 0;
            for (var person : city.citizens()) {
                if (!person.alive() || person.ageDays() < 8 * 24) continue;
                double labor = (.6 + person.skill(person.activity())) * person.health();
                switch (person.activity()) {
                    case GATHER -> {
                        double food = labor * (1 + v.biomass() * 5) * (1 + city.technology() * .08);
                        city.stocks().add(Resource.FOOD, food);
                        city.stocks().add(Resource.WOOD, labor * 1.2);
                        harvested += food;
                    }
                    case BUILD -> { city.stocks().add(Resource.STONE, labor); city.stocks().add(Resource.WOOD, labor * .5); }
                    case HEAL -> city.stocks().add(Resource.MEDICINE, labor * v.biomass() * .2);
                    case STUDY -> {
                        if (c.world().laws().allows(Laws.Domain.MAGIC)) city.stocks().add(Resource.MANA, labor * v.mana() * .25);
                        if (city.technology() >= 1) city.stocks().add(Resource.METAL, labor * v.ore() * .4);
                    }
                    default -> { }
                }
            }
            // Finite regional carrying capacity; overharvesting feeds back into the ecosystem.
            region.update(new Region.View(v.id(), v.latitude(), v.longitude(), v.elevation(), v.temperature(), v.moisture(), v.pressure(),
                    v.water(), v.biomass() - harvested / 15000, v.mana(), v.ore(), v.genome(), v.generation()));
            city.stocks().take(Resource.FOOD, city.stocks().get(Resource.FOOD) * .008);
        }
    }
    private static final class Disease extends CitySystem {
        Disease() { super("disease", WorldTime.TICKS_PER_DAY / 6, "pathogens,hosts", "crowding,water,health", "spread,recovery", "prevalence,medical_consumption"); }
        protected void act(LoopContext c, City city, double urgency) {
            double crowding = (double) city.population() / city.housing();
            double infection = .003 * Math.max(0, crowding - .8) + city.disease() * (1 - city.disease()) * .12 * crowding;
            double medicine = city.stocks().take(Resource.MEDICINE, city.population() * city.disease() * .03);
            double recovery = .06 * city.disease() + medicine / Math.max(1, city.population());
            double previous = city.disease();
            city.disease(previous + (infection - recovery) * c.days());
            if (previous <= .25 && city.disease() > .25) c.emit("crisis.disease", Long.toString(city.id()), "拥挤引发疫病", .9);
        }
    }
    private static final class Construction extends CitySystem {
        Construction() { super("construction", WorldTime.TICKS_PER_DAY, "parts,builders", "housing,materials", "shelter,maintenance", "construction_stages,ruins"); }
        protected double demand(LoopContext c, City city) { return 1; }
        protected void act(LoopContext c, City city, double urgency) {
            if (city.population() == 0) {
                city.buildings().forEach(b -> b.decay(.005)); return;
            }
            double labor = city.citizens().stream().filter(p -> p.alive() && p.activity() == BUILD && p.ageDays() >= 8 * 24)
                    .mapToDouble(p -> .5 + p.skill(BUILD)).sum();
            var unfinished = city.buildings().stream().filter(b -> b.stage().ordinal() < Building.Stage.COMPLETE.ordinal()).findFirst();
            if (unfinished.isPresent()) {
                var building = unfinished.get(); var before = building.stage();
                building.work(labor, city.stocks());
                if (before != building.stage()) c.emit("building.stage", Long.toString(city.id()), "建筑 " + building.id() + "：" + building.stage(), .35);
            } else if (city.housing() < city.population() * 1.5 && city.buildings().size() < 32) {
                var random = c.random(city.id());
                city.addBuilding(new Building(c.world().allocateId(), random.nextInt(5, 10), random.nextInt(5, 10)));
            }
        }
    }
    private static final class Politics extends CitySystem {
        Politics() { super("power_centers", WorldTime.TICKS_PER_DAY, "members,influence", "roles,crises", "survival,representation", "weighted_resolutions"); }
        protected void act(LoopContext c, City city, double urgency) {
            var centers = new ArrayList<City.Power>();
            for (var activity : Citizen.Activity.values()) {
                var members = city.citizens().stream().filter(p -> p.alive() && p.activity() == activity && p.repetitions() >= 6).toList();
                if (members.size() < 3) continue;
                double pressure = switch (activity) {
                    case GATHER -> 1 + Math.max(0, 3 - city.foodDays());
                    case BUILD -> 1 + Math.max(0, (double) city.population() / city.housing() - 1);
                    case HEAL -> 1 + city.disease() * 4;
                    default -> 1;
                };
                centers.add(new City.Power("" + members.get(0).id() + "·" + activity.name().toLowerCase(Locale.ROOT), activity, members.size() * pressure));
            }
            boolean changed = !city.powers().stream().map(City.Power::name).toList().equals(centers.stream().map(City.Power::name).toList());
            city.powers(centers);
            if (changed) c.emit("politics.changed", Long.toString(city.id()), "稳定分工形成新的权力关系", .55);
        }
    }
    private static final class Market extends CitySystem {
        Market() { super("market", WorldTime.TICKS_PER_DAY, "stocks,traders", "supply,demand,distance", "balance", "prices,trade"); }
        protected void act(LoopContext c, City city, double urgency) {
            for (var resource : Resource.values()) {
                double target = Math.max(8, city.population() * (resource == Resource.FOOD ? 4 : 2));
                double price = target / Math.max(1, city.stocks().get(resource));
                city.price(resource, city.price(resource) * .75 + Numbers.clamp(price, .1, 10) * .25);
            }
            // Each unordered pair is processed once. Barter has no invented global currency.
            for (var other : c.world().cities()) {
                if (other.id() <= city.id() || other.population() == 0 || Math.hypot(city.x() - other.x(), city.z() - other.z()) > 1200) continue;
                City buyer = city.foodDays() < other.foodDays() ? city : other;
                City seller = buyer == city ? other : city;
                if (buyer.foodDays() >= 3 || seller.foodDays() <= 5) continue;
                double food = Math.min(8, Math.min((seller.foodDays() - 5) * seller.population(), buyer.stocks().get(Resource.WOOD) * 2));
                if (food <= 0) continue;
                seller.stocks().take(Resource.FOOD, food); buyer.stocks().add(Resource.FOOD, food);
                buyer.stocks().take(Resource.WOOD, food / 2); seller.stocks().add(Resource.WOOD, food / 2);
                c.emit("trade.completed", Long.toString(buyer.id()), "以木材换取邻邦粮食", .25);
            }
        }
    }
    private static final class Knowledge extends CitySystem {
        Knowledge() { super("knowledge", WorldTime.TICKS_PER_DAY, "practitioners,knowledge", "skills,resources,laws", "innovation,inheritance", "technology,magic,regression"); }
        protected void act(LoopContext c, City city, double urgency) {
            var scholars = city.citizens().stream().filter(p -> p.alive() && p.activity() == STUDY && p.ageDays() >= 12 * 24).toList();
            double research = scholars.stream().mapToDouble(p -> p.skill(STUDY) + p.genome().cognition() * .05).sum() * .004;
            double nextTech = scholars.isEmpty() ? Math.max(0, city.technology() - .002) : city.technology();
            double nextMagic = scholars.isEmpty() ? Math.max(0, city.magic() - .003) : city.magic();
            boolean fed = city.foodDays() > 1;
            if (fed && c.world().laws().allows(Laws.Domain.TECHNOLOGY)) {
                Resource material = city.technology() < 1 ? Resource.STONE : Resource.METAL;
                if (city.stocks().consume(Map.of(material, research * 2))) nextTech += research;
            }
            if (fed && c.world().laws().allows(Laws.Domain.MAGIC) && city.stocks().consume(Map.of(Resource.MANA, research * 4))) nextMagic += research * 1.5;
            var before = city.era();
            city.knowledge(nextTech, nextMagic);
            if (city.era() != before) c.emit("knowledge.era", Long.toString(city.id()), city.name() + "进入" + city.era(), .95);
        }
    }
    private static final class Quests extends CitySystem {
        Quests() { super("quests", WorldTime.TICKS_PER_DAY / 6, "needs,claimants", "shortage,deadline", "resolve_gaps", "commissions,competition"); }
        protected double demand(LoopContext c, City city) { return 1; }
        protected void act(LoopContext c, City city, double urgency) {
            for (var quest : List.copyOf(c.world().quests())) {
                if (quest.city() != city.id() || !quest.active()) continue;
                if (c.tick() >= quest.deadline()) quest.close(Quest.Status.EXPIRED);
                else if (city.population() == 0) quest.close(Quest.Status.CANCELLED);
                else if (city.stocks().get(quest.resource()) >= quest.amount() + city.population() * 5)
                    quest.close(Quest.Status.COMPETITOR);
                if (!quest.active() && quest.claimant() != null) {
                    c.world().player(quest.claimant()).ifPresent(p -> p.penalize(1));
                    c.emit("quest.closed", Long.toString(quest.id()), quest.status().name(), .4);
                }
            }
            if (city.population() == 0 || c.world().quests().stream().anyMatch(q -> q.city() == city.id() && q.active())) return;
            Resource required = city.foodDays() < 3 ? Resource.FOOD
                    : city.housing() < city.population() && city.stocks().get(Resource.WOOD) < 32 ? Resource.WOOD
                    : city.disease() > .2 && city.stocks().get(Resource.MEDICINE) < 4 ? Resource.MEDICINE : null;
            if (required != null) {
                var quest = new Quest(c.world().allocateId(), city.id(), required, Math.min(64, Math.max(4, city.population())), c.tick() + 3 * WorldTime.TICKS_PER_DAY);
                c.world().addQuest(quest);
                c.emit("quest.offered", Long.toString(city.id()), "征集 " + quest.amount() + " " + required, .4);
            }
        }
    }
    private static final class Aggregation extends CitySystem {
        Aggregation() { super("civilization_records", WorldTime.TICKS_PER_DAY * 24, "cities,history", "population,fortune", "aggregate", "national_fortune"); }
        protected double demand(LoopContext c, City city) { return 1; }
        protected void act(LoopContext c, City city, double urgency) {
            for (var civilization : List.copyOf(c.world().civilizations())) {
                if (civilization.capital() != city.id()) continue;
                var members = civilization.cities().stream().map(c.world()::city).flatMap(Optional::stream).toList();
                int population = members.stream().mapToInt(City::population).sum();
                double cohesion = members.stream().mapToDouble(x -> x.fortune().cohesion() * x.population()).sum() / Math.max(1, population);
                double reserves = members.stream().mapToDouble(x -> x.fortune().reserves() * x.population()).sum() / Math.max(1, population);
                double adaptability = members.stream().mapToDouble(x -> x.fortune().adaptability() * x.population()).sum() / Math.max(1, population);
                c.world().putCivilization(new Civilization(civilization.id(), civilization.name(), civilization.capital(), civilization.cities(), new Fortune(cohesion, reserves, adaptability)));
            }
        }
    }
}
