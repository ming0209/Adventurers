package dev.adventurers.core.persistence;

import dev.adventurers.core.api.*;
import dev.adventurers.core.civilization.*;
import dev.adventurers.core.life.*;
import dev.adventurers.core.magic.*;
import dev.adventurers.core.player.*;
import dev.adventurers.core.world.*;
import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

/** Explicit, versioned data format. Never uses Java object deserialization. */
public final class WorldCodec {
    private static final int MAGIC = 0x41445652, VERSION = 1, MAX_BYTES = 64 * 1024 * 1024;
    private WorldCodec() {}
    public static byte[] encode(WorldModel world) throws IOException {
        var buffer = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(buffer)) {
            out.writeLong(world.seed()); out.writeLong(world.tick()); out.writeLong(world.nextId()); out.writeUTF(world.phase().name());
            out.writeInt(world.cityLimit()); out.writeInt(world.populationLimit());
            for (var domain : Laws.Domain.values()) out.writeBoolean(world.laws().allows(domain));
            out.writeDouble(world.laws().gravity()); out.writeDouble(world.laws().magicDensity());
            fortune(out, world.worldFortune()); out.writeDouble(world.worldWillReserve());
            out.writeInt(world.planet().columns()); out.writeInt(world.planet().rows());
            for (var region : world.planet().regions()) {
                var r = region.view();
                out.writeInt(r.id());
                for (double value : new double[]{r.latitude(),r.longitude(),r.elevation(),r.temperature(),r.moisture(),r.pressure(),r.water(),r.biomass(),r.mana(),r.ore()}) out.writeDouble(value);
                genome(out, r.genome()); out.writeInt(r.generation());
            }
            out.writeInt(world.cities().size());
            for (var city : world.cities()) city(out, city);
            out.writeInt(world.civilizations().size());
            for (var civ : world.civilizations()) {
                out.writeLong(civ.id()); out.writeUTF(civ.name()); out.writeLong(civ.capital()); out.writeInt(civ.cities().size());
                for (long id : civ.cities()) out.writeLong(id);
                fortune(out, civ.fortune());
            }
            out.writeInt(world.players().size());
            for (var player : world.players()) player(out, player);
            out.writeInt(world.quests().size());
            for (var quest : world.quests()) {
                out.writeLong(quest.id()); out.writeLong(quest.city()); out.writeUTF(quest.resource().name());
                out.writeInt(quest.amount()); out.writeLong(quest.deadline()); out.writeUTF(quest.status().name());
                out.writeBoolean(quest.claimant() != null); if (quest.claimant() != null) uuid(out, quest.claimant());
            }
            out.writeInt(world.history().size());
            for (var event : world.history()) {
                out.writeLong(event.tick()); out.writeUTF(event.type()); out.writeUTF(event.subject()); out.writeUTF(event.detail()); out.writeDouble(event.importance());
            }
        }
        byte[] payload = buffer.toByteArray();
        if (payload.length > MAX_BYTES) throw new IOException("World snapshot exceeds budget");
        var crc = new CRC32(); crc.update(payload);
        buffer = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(buffer)) {
            out.writeInt(MAGIC); out.writeInt(VERSION); out.writeInt(payload.length); out.writeLong(crc.getValue()); out.write(payload);
        }
        return buffer.toByteArray();
    }
    public static WorldModel decode(byte[] data) throws IOException {
        if (data.length > MAX_BYTES + 20) throw new IOException("Oversized snapshot");
        try (var envelope = new DataInputStream(new ByteArrayInputStream(data))) {
            if (envelope.readInt() != MAGIC) throw new IOException("Not an Adventurers snapshot");
            if (envelope.readInt() != VERSION) throw new IOException("Unsupported snapshot version; original save preserved");
            int size = count(envelope, MAX_BYTES); long checksum = envelope.readLong();
            if (envelope.available() != size) throw new IOException("Truncated or trailing snapshot data");
            byte[] payload = envelope.readNBytes(size);
            var crc = new CRC32(); crc.update(payload);
            if (crc.getValue() != checksum) throw new IOException("Snapshot checksum mismatch");
            try (var in = new DataInputStream(new ByteArrayInputStream(payload))) {
                long seed = in.readLong(), tick = in.readLong(), nextId = in.readLong();
                var phase = WorldModel.Phase.valueOf(in.readUTF()); int cityLimit = in.readInt(), populationLimit = in.readInt();
                var domains = EnumSet.noneOf(Laws.Domain.class);
                for (var domain : Laws.Domain.values()) if (in.readBoolean()) domains.add(domain);
                var laws = new Laws(domains, in.readDouble(), in.readDouble());
                var fortune = fortune(in); double reserve = in.readDouble();
                int columns = count(in, 128), rows = count(in, 64);
                var regions = new ArrayList<Region>();
                for (int i = 0; i < columns * rows; i++) {
                    regions.add(new Region(new Region.View(in.readInt(), in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble(),
                            in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble(), in.readDouble(), genome(in), in.readInt())));
                }
                var world = new WorldModel(seed, new Planet(columns, rows, regions), laws, cityLimit, populationLimit);
                world.restoreClock(tick, nextId, phase); world.worldWill(fortune, reserve);
                int cities = count(in, 64);
                for (int i = 0; i < cities; i++) {
                    var city = city(in);
                    if (world.city(city.id()).isPresent()) throw new IOException("Duplicate city id");
                    world.putCity(city);
                }
                int civilizations = count(in, 64);
                for (int i = 0; i < civilizations; i++) {
                    long id = in.readLong(); String name = in.readUTF(); long capital = in.readLong(); int sizeCities = count(in, 64);
                    var ids = new ArrayList<Long>(); for (int j = 0; j < sizeCities; j++) ids.add(in.readLong());
                    if (world.civilizations().stream().anyMatch(c -> c.id() == id)) throw new IOException("Duplicate civilization id");
                    world.putCivilization(new Civilization(id, name, capital, ids, fortune(in)));
                }
                int players = count(in, 100_000);
                for (int i = 0; i < players; i++) {
                    var player = player(in);
                    if (world.player(player.id()).isPresent()) throw new IOException("Duplicate player id");
                    world.putPlayer(player);
                }
                int quests = count(in, 4096);
                for (int i = 0; i < quests; i++) {
                    var quest = new Quest(in.readLong(), in.readLong(), Resource.valueOf(in.readUTF()), in.readInt(), in.readLong());
                    var status = Quest.Status.valueOf(in.readUTF()); var claimant = in.readBoolean() ? uuid(in) : null;
                    if (world.quest(quest.id()).isPresent()) throw new IOException("Duplicate quest id");
                    quest.restore(status, claimant); world.addQuest(quest);
                }
                int events = count(in, 256);
                for (int i = 0; i < events; i++) world.record(new WorldEvent(in.readLong(),in.readUTF(),in.readUTF(),in.readUTF(),in.readDouble()));
                if (in.available() != 0) throw new IOException("Trailing payload data");
                validate(world);
                return world;
            }
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) { throw new IOException("Invalid snapshot data", e); }
    }
    private static void validate(WorldModel world) throws IOException {
        var ids = new HashSet<Long>();
        for (var civ : world.civilizations()) checkId(ids, civ.id(), world.nextId());
        for (var city : world.cities()) {
            checkId(ids, city.id(), world.nextId());
            if (city.region() < 0 || city.region() >= world.planet().regions().size()) throw new IOException("Missing city region");
            if (world.civilizations().stream().noneMatch(c -> c.id() == city.civilization() && c.cities().contains(city.id()))) throw new IOException("Missing civilization");
            for (var person : city.citizens()) checkId(ids, person.id(), world.nextId());
            for (var building : city.buildings()) checkId(ids, building.id(), world.nextId());
        }
        for (var quest : world.quests()) { checkId(ids, quest.id(), world.nextId()); if (world.city(quest.city()).isEmpty()) throw new IOException("Missing quest city"); }
        for (var civ : world.civilizations()) if (!civ.cities().contains(civ.capital()) || civ.cities().stream().anyMatch(id -> world.city(id).isEmpty())) throw new IOException("Invalid civilization membership");
        for (var player : world.players()) if (world.city(player.city()).isEmpty()) throw new IOException("Missing player city");
        if (world.population() > world.populationLimit()) throw new IOException("Population budget exceeded");
    }
    private static void checkId(Set<Long> ids, long id, long nextId) throws IOException {
        if (id < 1 || id >= nextId || !ids.add(id)) throw new IOException("Invalid or duplicate entity id");
    }
    private static void city(DataOutputStream out, City city) throws IOException {
        out.writeLong(city.id()); out.writeLong(city.civilization()); out.writeInt(city.region()); out.writeUTF(city.name()); out.writeInt(city.x()); out.writeInt(city.z());
        fortune(out, city.fortune()); out.writeDouble(city.disease()); out.writeDouble(city.technology()); out.writeDouble(city.magic());
        for (var resource : Resource.values()) { out.writeDouble(city.stocks().get(resource)); out.writeDouble(city.price(resource)); }
        out.writeInt(city.citizens().size());
        for (var person : city.citizens()) {
            out.writeLong(person.id()); genome(out, person.genome()); out.writeDouble(person.ageDays());
            out.writeDouble(person.health()); out.writeDouble(person.hunger()); out.writeDouble(person.stress()); out.writeUTF(person.activity().name()); out.writeInt(person.repetitions());
            for (var activity : Citizen.Activity.values()) out.writeDouble(person.skill(activity));
            out.writeInt(person.memory().entries().size());
            for (var memory : person.memory().entries()) { out.writeLong(memory.tick()); out.writeUTF(memory.event()); out.writeDouble(memory.importance()); }
        }
        out.writeInt(city.buildings().size());
        for (var building : city.buildings()) {
            out.writeLong(building.id()); out.writeInt(building.width()); out.writeInt(building.depth()); out.writeUTF(building.stage().name()); out.writeDouble(building.progress()); out.writeDouble(building.integrity());
        }
        out.writeInt(city.powers().size());
        for (var power : city.powers()) { out.writeUTF(power.name()); out.writeUTF(power.interest().name()); out.writeDouble(power.weight()); }
    }
    private static City city(DataInputStream in) throws IOException {
        var city = new City(in.readLong(), in.readLong(), in.readInt(), in.readUTF(), in.readInt(), in.readInt());
        city.fortune(fortune(in)); city.disease(in.readDouble()); city.knowledge(in.readDouble(), in.readDouble());
        for (var resource : Resource.values()) { city.stocks().add(resource, in.readDouble()); city.price(resource, in.readDouble()); }
        int people = count(in, 100_000);
        for (int i = 0; i < people; i++) {
            var person = new Citizen(in.readLong(), city.id(), genome(in), in.readDouble());
            double health = in.readDouble(), hunger = in.readDouble(), stress = in.readDouble();
            var activity = Citizen.Activity.valueOf(in.readUTF()); int repetitions = count(in, Integer.MAX_VALUE);
            var skills = new EnumMap<Citizen.Activity, Double>(Citizen.Activity.class);
            for (var a : Citizen.Activity.values()) skills.put(a, in.readDouble());
            person.restore(health, hunger, stress, activity, repetitions, skills);
            int memories = count(in, 16);
            for (int j = 0; j < memories; j++) {
                long tick = in.readLong(); person.memory().remember(new Memory.Entry(tick, in.readUTF(), in.readDouble()), tick);
            }
            city.addCitizen(person);
        }
        int buildings = count(in, 64);
        for (int i = 0; i < buildings; i++) {
            var building = new Building(in.readLong(), in.readInt(), in.readInt());
            building.restore(Building.Stage.valueOf(in.readUTF()), in.readDouble(), in.readDouble()); city.addBuilding(building);
        }
        int powers = count(in, 64); var centers = new ArrayList<City.Power>();
        for (int i = 0; i < powers; i++) centers.add(new City.Power(in.readUTF(), Citizen.Activity.valueOf(in.readUTF()), in.readDouble()));
        city.powers(centers); return city;
    }
    private static void player(DataOutputStream out, PlayerProfile p) throws IOException {
        uuid(out, p.id()); out.writeLong(p.city()); out.writeUTF(p.origin().name()); out.writeInt(p.incarnation());
        out.writeDouble(p.mana()); out.writeDouble(p.mentalCapacity()); out.writeDouble(p.consciousness()); out.writeDouble(p.reputation());
        out.writeLong(p.lastMeditation()); out.writeLong(p.lastCast()); out.writeBoolean(p.banner());
        for (var element : Element.values()) out.writeBoolean(p.knowledge().contains(element));
        out.writeInt(p.spells().size()); for (var spell : p.spells().values()) spell(out, spell);
    }
    private static PlayerProfile player(DataInputStream in) throws IOException {
        var p = new PlayerProfile(uuid(in), in.readLong(), PlayerProfile.Origin.valueOf(in.readUTF()), count(in, Integer.MAX_VALUE));
        double mana = in.readDouble(), mental = in.readDouble(), consciousness = in.readDouble(), reputation = in.readDouble();
        long meditate = in.readLong(), cast = in.readLong(); boolean banner = in.readBoolean();
        var knowledge = EnumSet.noneOf(Element.class); for (var e : Element.values()) if (in.readBoolean()) knowledge.add(e);
        p.restore(mana, mental, consciousness, reputation, meditate, cast, banner, knowledge);
        int spells = count(in, 32); for (int i = 0; i < spells; i++) p.remember(spell(in)); return p;
    }
    private static void spell(DataOutputStream out, Spell s) throws IOException {
        out.writeUTF(s.name()); out.writeUTF(s.effect().name()); out.writeInt(s.runes().size());
        for (var r : s.runes()) { out.writeUTF(r.element().name()); out.writeUTF(r.polarity().name()); out.writeUTF(r.kind().name()); out.writeInt(r.layer()); }
        var c = s.circle(); out.writeBoolean(c.eye()); out.writeBoolean(c.core()); out.writeBoolean(c.ring()); out.writeBoolean(c.skeleton());
        out.writeDouble(c.capacity()); out.writeDouble(c.quality()); out.writeDouble(c.conductivity()); out.writeDouble(c.affinity());
    }
    private static Spell spell(DataInputStream in) throws IOException {
        String name = in.readUTF(); var effect = Spell.Effect.valueOf(in.readUTF()); int size = count(in, 64);
        var runes = new ArrayList<Spell.Rune>();
        for (int i = 0; i < size; i++) runes.add(new Spell.Rune(Element.valueOf(in.readUTF()), Element.Polarity.valueOf(in.readUTF()), Spell.Kind.valueOf(in.readUTF()), in.readInt()));
        return new Spell(name, effect, runes, new Spell.Circle(in.readBoolean(),in.readBoolean(),in.readBoolean(),in.readBoolean(),in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble()));
    }
    private static int count(DataInputStream in, int max) throws IOException {
        int size = in.readInt(); if (size < 0 || size > max) throw new IOException("Invalid collection size: " + size); return size;
    }
    private static void fortune(DataOutputStream out, Fortune f) throws IOException { out.writeDouble(f.cohesion()); out.writeDouble(f.reserves()); out.writeDouble(f.adaptability()); }
    private static Fortune fortune(DataInputStream in) throws IOException { return new Fortune(in.readDouble(),in.readDouble(),in.readDouble()); }
    private static void genome(DataOutputStream out, Genome g) throws IOException {
        for (double value : new double[]{g.warmth(),g.waterNeed(),g.size(),g.mobility(),g.cognition(),g.sociality(),g.defense(),g.manaAffinity()}) out.writeDouble(value);
    }
    private static Genome genome(DataInputStream in) throws IOException { return new Genome(in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble(),in.readDouble()); }
    private static void uuid(DataOutputStream out, UUID id) throws IOException { out.writeLong(id.getMostSignificantBits()); out.writeLong(id.getLeastSignificantBits()); }
    private static UUID uuid(DataInputStream in) throws IOException { return new UUID(in.readLong(),in.readLong()); }
}
