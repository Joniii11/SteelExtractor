package com.steelextractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.steelextractor.SteelExtractor
import com.steelextractor.mixin.MatchingBlocksPredicateAccessor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.component.BlockTransformers
import net.minecraft.world.level.levelgen.blockpredicates.MatchingBlocksPredicate
import net.minecraft.world.level.levelgen.feature.stateproviders.CopyPropertiesProvider
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider

class Strippables : SteelExtractor.Extractor {
    override fun fileName(): String {
        return "steel-core/build/strippables.json"
    }

    @Suppress("CAST_NEVER_SUCCEEDS")
    override fun extract(server: MinecraftServer): JsonElement {
        val topLevelJson = JsonObject()
        val axe = server.registryAccess()
            .lookupOrThrow(Registries.BLOCK_TRANSFORMER)
            .getOrThrow(BlockTransformers.AXE)
            .value()

        for (transform in axe.transforms()) {
            val provider = transform.blockStateProvider().value() as? RuleBasedStateProvider
                ?: error("Unexpected axe transform provider: ${transform.blockStateProvider().value()}")
            for (rule in provider.rules()) {
                val predicate = rule.ifTrue() as? MatchingBlocksPredicate
                    ?: error("Unexpected axe transform predicate: ${rule.ifTrue()}")
                val target = rule.`then`().value() as? CopyPropertiesProvider
                    ?: error("Unexpected axe transform target: ${rule.`then`().value()}")
                val source = target.source().value() as? SimpleStateProvider
                    ?: error("Unexpected axe transform source: ${target.source().value()}")
                val stripped = source.state().block

                // Fabric Mixin injects this accessor into MatchingBlocksPredicate at runtime.
                for (normal in (predicate as MatchingBlocksPredicateAccessor).blocks) {
                    topLevelJson.addProperty(
                        BuiltInRegistries.BLOCK.getKey(normal.value()).path,
                        BuiltInRegistries.BLOCK.getKey(stripped).path
                    )
                }
            }
        }

        return topLevelJson
    }
}
