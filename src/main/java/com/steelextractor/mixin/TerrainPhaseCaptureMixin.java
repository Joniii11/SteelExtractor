package com.steelextractor.mixin;

import com.steelextractor.BlockHashResult;
import com.steelextractor.ChunkStageHashStorage;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.Set;

@Mixin(NoiseBasedChunkGenerator.class)
public class TerrainPhaseCaptureMixin {
    @Inject(method = "buildSurface", at = @At("RETURN"))
    private void captureSurface(
        ChunkAccess chunk,
        NoiseChunk noiseChunk,
        RandomState randomState,
        BiomeManager biomeManager,
        Set<Holder<Biome>> possibleBiomes,
        MaterialRule materialRule,
        CallbackInfo ci
    ) {
        capture(chunk, "minecraft:surface");
    }

    @Inject(method = "generateCarvers", at = @At("RETURN"))
    private void captureCarvers(
        ChunkAccess chunk,
        Blender blender,
        NoiseChunk noiseChunk,
        RandomState randomState,
        BiomeManager biomeManager,
        WorldGenRegion carverBiomeRegion,
        MaterialRule materialRule,
        CallbackInfo ci
    ) {
        capture(chunk, "minecraft:carvers");
    }

    private static void capture(ChunkAccess chunk, String stage) {
        String dimension = ChunkStageHashStorage.INSTANCE.getCurrentDimension();
        if (!ChunkStageHashStorage.INSTANCE.isTracking(chunk.getPos(), dimension)) {
            return;
        }

        LevelChunkSection[] sections = chunk.getSections();
        if (ChunkStageHashStorage.INSTANCE.getEnableBinaryDump()) {
            BlockHashResult result = ChunkStageHashStorage.INSTANCE.computeBlockHashWithData(Arrays.asList(sections));
            ChunkStageHashStorage.INSTANCE.storeHash(chunk.getPos(), dimension, stage, result.getHash());
            ChunkStageHashStorage.INSTANCE.storeBlockData(chunk.getPos(), dimension, stage, result.getSectionData());
        } else {
            String hash = ChunkStageHashStorage.INSTANCE.computeBlockHash(Arrays.asList(sections));
            ChunkStageHashStorage.INSTANCE.storeHash(chunk.getPos(), dimension, stage, hash);
        }
    }
}
