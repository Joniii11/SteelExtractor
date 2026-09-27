package com.steelextractor.extractors

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.common.hash.HashCode
import com.mojang.serialization.JsonOps
import com.steelextractor.SteelExtractor
import net.minecraft.core.component.DataComponents
import net.minecraft.core.component.TypedDataComponent
import net.minecraft.core.component.BlockTransformer
import net.minecraft.nbt.NbtOps
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.RegistryOps
import net.minecraft.server.MinecraftServer
import net.minecraft.util.HashOps
import net.minecraft.world.item.AdventureModePredicate

/**
 * Extracts Vanilla component-hash fixtures used by Steel's hashed item-patch tests.
 */
class ComponentHashes : SteelExtractor.Extractor {
    override fun fileName(): String = "steel-registry/test_assets/component_hashes.json"

    override fun extract(server: MinecraftServer): JsonElement {
        val root = JsonObject()
        val hashes = JsonObject()
        val ops = server.registryAccess().createSerializationContext(HashOps.CRC32C_INSTANCE)

        for (item in BuiltInRegistries.ITEM) {
            val transformer = item.components().get(DataComponents.BLOCK_TRANSFORMER) ?: continue
            val component = TypedDataComponent(DataComponents.BLOCK_TRANSFORMER, transformer)
            val hash = component.encodeValue(ops)
                .getOrThrow { message -> IllegalArgumentException("Failed to hash $component: $message") }
                .let { value -> (value as HashCode).asInt() }
            hashes.addProperty(BuiltInRegistries.ITEM.getKey(item).toString(), hash)
        }

        root.add("block_transformer", hashes)

        val potteryPatternHashes = JsonObject()
        for (item in BuiltInRegistries.ITEM) {
            val pattern = item.components().get(DataComponents.PROVIDES_POTTERY_PATTERN) ?: continue
            val component = TypedDataComponent(DataComponents.PROVIDES_POTTERY_PATTERN, pattern)
            val hash = component.encodeValue(ops)
                .getOrThrow { message -> IllegalArgumentException("Failed to hash $component: $message") }
                .let { value -> (value as HashCode).asInt() }
            potteryPatternHashes.addProperty(BuiltInRegistries.ITEM.getKey(item).toString(), hash)
        }
        root.add("provides_pottery_pattern", potteryPatternHashes)

        val adventureHashes = JsonObject()
        val adventureOps = RegistryOps.create(JsonOps.INSTANCE, server.registryAccess())
        val adventurePredicate = AdventureModePredicate.CODEC.parse(
            adventureOps,
            JsonParser.parseString(
                """
                [
                  {
                    "blocks": ["minecraft:oak_log"],
                    "state": {
                      "axis": "y",
                      "distance": {"min": "1", "max": "7"}
                    },
                    "nbt": "{id:\"minecraft:chest\"}",
                    "components": {"minecraft:max_damage": 1561},
                    "predicates": {
                      "minecraft:max_stack_size": {},
                      "minecraft:damage": {}
                    }
                  },
                  {"blocks": "#minecraft:mineable/axe"}
                ]
                """.trimIndent()
            )
        ).getOrThrow { message ->
            IllegalArgumentException("Failed to decode adventure predicate fixture: $message")
        }
        val adventureComponent = TypedDataComponent(DataComponents.CAN_BREAK, adventurePredicate)
        val adventureHash = adventureComponent.encodeValue(ops)
            .getOrThrow { message -> IllegalArgumentException("Failed to hash $adventureComponent: $message") }
            .let { value -> (value as HashCode).asInt() }
        adventureHashes.addProperty("complex", adventureHash)

        val booleanComponentPredicate = AdventureModePredicate.CODEC.parse(
            adventureOps,
            JsonParser.parseString(
                """
                [
                  {
                    "components": {
                      "minecraft:enchantment_glint_override": false
                    }
                  }
                ]
                """.trimIndent()
            )
        ).getOrThrow { message ->
            IllegalArgumentException("Failed to decode boolean adventure predicate fixture: $message")
        }
        val booleanComponent = TypedDataComponent(DataComponents.CAN_BREAK, booleanComponentPredicate)
        val booleanComponentHash = booleanComponent.encodeValue(ops)
            .getOrThrow { message -> IllegalArgumentException("Failed to hash $booleanComponent: $message") }
            .let { value -> (value as HashCode).asInt() }
        adventureHashes.addProperty("boolean_component", booleanComponentHash)
        root.add("adventure_mode_predicate", adventureHashes)

        val allVariantsFixture = JsonParser.parseString(
                """
                [
                  {
                    "block_state_provider": {
                      "type": "minecraft:rule_based",
                      "fallback": {
                        "type": "minecraft:rotated",
                        "state": {"id": "minecraft:oak_log"}
                      },
                      "rules": [
                        {
                          "if_true": {
                            "type": "minecraft:matching_blocks",
                            "offset": [1, 0, -1],
                            "blocks": ["minecraft:stone", "minecraft:dirt"]
                          },
                          "then": {"id": "minecraft:dirt"}
                        },
                        {
                          "if_true": {
                            "type": "minecraft:matching_block_tag",
                            "offset": [1, 0, 0],
                            "tag": "minecraft:logs"
                          },
                          "then": {
                            "type": "minecraft:weighted",
                            "entries": [
                              {"data": {"id": "minecraft:stone"}, "weight": 1},
                              {"data": {"id": "minecraft:dirt"}, "weight": 2}
                            ]
                          }
                        },
                        {
                          "if_true": {
                            "type": "minecraft:matching_fluids",
                            "offset": [0, 1, 0],
                            "fluids": ["minecraft:water", "minecraft:lava"]
                          },
                          "then": {
                            "type": "minecraft:noise_threshold",
                            "seed": 12,
                            "noise": {"base_octave": 0, "amplitudes": [1.0]},
                            "scale": 1.0,
                            "threshold": 0.0,
                            "high_chance": 0.5,
                            "default_state": {"id": "minecraft:stone"},
                            "low_states": [{"id": "minecraft:dirt"}],
                            "high_states": [{"id": "minecraft:granite"}]
                          }
                        },
                        {
                          "if_true": {
                            "type": "minecraft:matching_biomes",
                            "biomes": ["minecraft:plains", "minecraft:desert"]
                          },
                          "then": {
                            "type": "minecraft:noise",
                            "seed": 13,
                            "noise": {"base_octave": 0, "amplitudes": [1.0]},
                            "scale": 1.0,
                            "states": [{"id": "minecraft:stone"}, {"id": "minecraft:dirt"}]
                          }
                        },
                        {
                          "if_true": {
                            "type": "minecraft:has_sturdy_face",
                            "offset": [0, -1, 0],
                            "direction": "up"
                          },
                          "then": {
                            "type": "minecraft:dual_noise",
                            "variety": {"min_inclusive": 1, "max_inclusive": 2},
                            "slow_noise": {"base_octave": -1, "amplitudes": [1.0]},
                            "slow_scale": 1.0,
                            "seed": 14,
                            "noise": {"base_octave": 0, "amplitudes": [1.0]},
                            "scale": 1.0,
                            "states": [{"id": "minecraft:stone"}, {"id": "minecraft:dirt"}]
                          }
                        },
                        {
                          "if_true": {"type": "minecraft:solid", "offset": [0, 0, 0]},
                          "then": {
                            "type": "minecraft:randomized_int",
                            "source": {"id": "minecraft:wheat"},
                            "property": "age",
                            "values": 0
                          }
                        },
                        {
                          "if_true": {"type": "minecraft:replaceable", "offset": [0, 0, 0]},
                          "then": {
                            "type": "minecraft:copy_properties",
                            "source": {"id": "minecraft:oak_log"}
                          }
                        },
                        {
                          "if_true": {
                            "type": "minecraft:would_survive",
                            "offset": [0, 1, 0],
                            "state": {"id": "minecraft:torch"}
                          },
                          "then": {"id": "minecraft:torch"}
                        },
                        {
                          "if_true": {"type": "minecraft:inside_world_bounds", "offset": [0, 0, 0]},
                          "then": {"id": "minecraft:stone"}
                        },
                        {
                          "if_true": {
                            "type": "minecraft:any_of",
                            "predicates": [
                              {"type": "minecraft:true"},
                              {"type": "minecraft:matching_blocks", "blocks": "minecraft:dirt"}
                            ]
                          },
                          "then": {"id": "minecraft:stone"}
                        },
                        {
                          "if_true": {
                            "type": "minecraft:all_of",
                            "predicates": [
                              {"type": "minecraft:true"},
                              {"type": "minecraft:matching_blocks", "blocks": "minecraft:dirt"}
                            ]
                          },
                          "then": {"id": "minecraft:stone"}
                        },
                        {
                          "if_true": {
                            "type": "minecraft:not",
                            "predicate": {"type": "minecraft:matching_blocks", "blocks": "minecraft:bedrock"}
                          },
                          "then": {"id": "minecraft:stone"}
                        },
                        {
                          "if_true": {"type": "minecraft:true"},
                          "then": {"id": "minecraft:stone"}
                        },
                        {
                          "if_true": {"type": "minecraft:unobstructed", "offset": [100, 0, 0]},
                          "then": {"id": "minecraft:stone"}
                        },
                        {
                          "if_true": {
                            "type": "minecraft:height_range",
                            "min_inclusive": {"absolute": 0},
                            "max_inclusive": {"above_bottom": 32}
                          },
                          "then": {"id": "minecraft:stone"}
                        }
                      ]
                    },
                    "consume_on_use": false,
                    "item_damage_per_use": 2
                  }
                ]
                """.trimIndent()
            )
        val allVariantsTransformer = BlockTransformer(
            listOf(
                BlockTransformer.BlockTransformData.CODEC.parse(adventureOps, allVariantsFixture.asJsonArray[0])
                    .getOrThrow { message ->
                        IllegalArgumentException("Failed to decode all-variants block transformer fixture: $message")
                    }
            )
        )
        val allVariants = JsonObject()
        allVariants.add(
            "codec",
            BlockTransformer.DIRECT_CODEC.encodeStart(adventureOps, allVariantsTransformer)
                .getOrThrow { message ->
                    IllegalArgumentException("Failed to encode all-variants block transformer fixture: $message")
                }
        )
        val transformerNbtOps = server.registryAccess().createSerializationContext(NbtOps.INSTANCE)
        allVariants.addProperty(
            "snbt",
            BlockTransformer.DIRECT_CODEC.encodeStart(transformerNbtOps, allVariantsTransformer)
                .getOrThrow { message ->
                    IllegalArgumentException("Failed to encode all-variants block transformer NBT fixture: $message")
                }
                .toString()
        )
        root.add("block_transformer_all_variants", allVariants)

        return root
    }
}
