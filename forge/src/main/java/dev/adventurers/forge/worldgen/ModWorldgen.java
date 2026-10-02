package dev.adventurers.forge.worldgen;

import com.mojang.serialization.MapCodec;
import dev.adventurers.forge.AdventurersMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.DeferredRegister;

public final class ModWorldgen {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS=DeferredRegister.create(Registries.CHUNK_GENERATOR,AdventurersMod.ID);
    public static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOMES=DeferredRegister.create(Registries.BIOME_SOURCE,AdventurersMod.ID);
    static {
        GENERATORS.register("planet",()->PlanetChunkGenerator.CODEC);
        BIOMES.register("planet",()->PlanetBiomeSource.CODEC);
    }
    private ModWorldgen() {}
}
