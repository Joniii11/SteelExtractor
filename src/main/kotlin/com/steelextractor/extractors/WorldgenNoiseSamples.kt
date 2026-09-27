package com.steelextractor.extractors

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.steelextractor.SteelExtractor
import net.minecraft.core.Holder
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.util.RandomSource
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.LegacyRandomSource
import net.minecraft.world.level.levelgen.densityfunction.DensityFunction
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext
import net.minecraft.world.level.levelgen.synth.BlendedNoise
import net.minecraft.world.level.levelgen.synth.Noise
import net.minecraft.world.level.levelgen.synth.NormalNoise

/** Samples actual Java float results, including coordinates outside the exact integer range of float. */
class WorldgenNoiseSamples : SteelExtractor.Extractor {
    override fun fileName() = "steel-worldgen/test_assets/noise_samples.json"

    override fun extract(server: MinecraftServer): JsonElement {
        val samples = JsonArray()
        val positions = listOf(
            intArrayOf(0, -56, -10000), intArrayOf(20000068, 296, -19999796),
            intArrayOf(0, 0, 0), intArrayOf(1, 1, 1), intArrayOf(-1, -1, -1),
            intArrayOf(16777217, 100, -16777217), intArrayOf(29999980, -64, -29999980),
            intArrayOf(54545, 63, 34234), intArrayOf(-842, 319, 425)
        )
        for (seed in listOf(0L, 13579L, -1L)) {
            val parameters = NormalNoise.createParity(-7, 1.0, 1.0)
            val normal = parameters.create(LegacyRandomSource(seed))
            val nether = parameters.createForLegacyNetherBiome(LegacyRandomSource(seed))
            val context = object : DensityFunction.CompileContext {
                override fun createNoiseSampler(parameters: Holder<NormalNoise>): Noise =
                    parameters.value().create(LegacyRandomSource(seed))
                override fun createRandom(id: Identifier): RandomSource = LegacyRandomSource(seed)
                override fun createEndIslandRandom(): RandomSource = LegacyRandomSource(seed)
            }
            val blended = BlendedNoise(0.25, 0.125, 80.0, 160.0, 8.0).compileSampler(context)
            for (pos in positions) {
                val (x, y, z) = pos
                fun record(sampler: String, value: Float) {
                    samples.add(JsonObject().apply {
                        addProperty("sampler", sampler)
                        addProperty("seed", seed)
                        addProperty("x", x)
                        addProperty("y", y)
                        addProperty("z", z)
                        addProperty("value_bits", value.toRawBits().toLong() and 0xffffffffL)
                    })
                }
                record("normal", normal.get(x.toDouble(), y.toDouble(), z.toDouble()))
                record("legacy_nether", nether.get(x.toDouble(), y.toDouble(), z.toDouble()))
                record("blended", blended.sampleValue(SamplerContext.EMPTY_UNCACHED, x, y, z))
                if (seed == 0L) {
                    record("frozen_temperature", Biome.FROZEN_TEMPERATURE_NOISE.get(x * 0.05, z * 0.05))
                }
            }
        }
        return samples
    }
}
