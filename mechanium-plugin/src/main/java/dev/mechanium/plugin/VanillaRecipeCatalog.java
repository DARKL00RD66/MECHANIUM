package dev.mechanium.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.CookingRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Discovers the vanilla recipes actually registered by the running Paper server.
 * The catalog intentionally contains recipe outputs that are safe for historical
 * progression management; combat weapons and unsupported campfire recipes are omitted.
 */
final class VanillaRecipeCatalog {
    private VanillaRecipeCatalog() { }

    record Entry(NamespacedKey key, Material result, String type, String suggestedEraId) {
        String displayName() {
            return result.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        }
    }

    static List<Entry> discover() {
        List<Entry> entries = new ArrayList<>();
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (!(recipe instanceof Keyed keyed)) continue;

            NamespacedKey key = keyed.getKey();
            if (!"minecraft".equals(key.getNamespace())) continue;

            Material result = recipe.getResult() == null ? Material.AIR : recipe.getResult().getType();
            if (result.isAir() || isCombatOutput(result)) continue;

            // Paper exposes no cancellable campfire-completion event carrying the recipe key.
            // Keep unsupported station mechanics out of the editable catalog rather than
            // displaying a rule that the plugin cannot consistently enforce.
            if (recipe instanceof CampfireRecipe) continue;

            String type = typeOf(recipe);
            entries.add(new Entry(key, result, type,
                    recommendedEraId(key.getKey(), result)));
        }

        entries.sort(Comparator.comparing(entry -> entry.key().toString()));
        return List.copyOf(entries);
    }

    static String recommendedEraId(String recipePath, Material result) {
        String path = recipePath == null ? "" : recipePath.toLowerCase(Locale.ROOT);
        String material = result == null ? "" : result.name();

        // Magical/end-dimension content belongs to the optional mythic branch, not to
        // ordinary historical industry.
        if (isMythicOutput(material, path)) return "mythic_age";

        // Keep the last two vanilla progression tiers distinct from ordinary metalworking.
        if (material.startsWith("NETHERITE_") || path.contains("netherite")) {
            return "speculative_future";
        }
        if (material.startsWith("SHULKER_") || material.equals("ELYTRA")
                || path.contains("shulker") || path.contains("elytra")) {
            return "space_age";
        }

        // Programmable automation is later than basic electrical logic.
        if (material.equals("CRAFTER") || material.equals("OBSERVER")
                || material.equals("CALIBRATED_SCULK_SENSOR")
                || containsAny(path, "crafter", "observer", "calibrated_sculk")) {
            return "digital_age";
        }
        if (containsAny(material, "SCULK_SENSOR", "SCULK_CATALYST", "SCULK_SHRIEKER", "ECHO_SHARD")
                || containsAny(path, "sculk_sensor", "sculk_catalyst", "sculk_shrieker", "echo_shard")) {
            return "information_age";
        }

        // Electrical infrastructure and control circuits are kept separate from factory machinery.
        if (isElectricalOutput(material) || containsAny(path,
                "redstone_lamp", "repeater", "comparator", "daylight_detector",
                "lightning_rod", "copper_bulb")) {
            return "electrification";
        }

        if (isIndustrialOutput(result) || containsAny(path,
                "piston", "hopper", "dispenser", "dropper", "minecart", "rail",
                "blast_furnace", "smoker")) {
            return "industrial_revolution";
        }

        if (isModernOutput(material, path)) return "modern";

        // Precision navigation, optics and diamond-grade equipment are later than basic metalworking.
        if (isRenaissanceOutput(material, path)) return "renaissance";

        if (isMedievalOutput(material, path)) return "medieval";

        // Explicitly reserve Antiquity for early materials, hand tools, basic food and simple building.
        if (isAncientOutput(material, path)) return "ancient";

        // Unknown recipes should not silently become ancient. Renaissance is the neutral review tier;
        // administrators can refine the generated rule in the recipe panel.
        return "renaissance";
    }

    /**
     * Reproduces the pre-rebalance suggestions so startup migration can identify values
     * that were seeded automatically by the previous classifier.
     */
    static String legacyRecommendedEraId(String recipePath, Material result) {
        String path = recipePath == null ? "" : recipePath.toLowerCase(Locale.ROOT);
        String material = result == null ? "" : result.name();

        if (result == Material.ENCHANTING_TABLE || path.equals("enchanting_table")) return "mythic_age";
        if (material.startsWith("NETHERITE_") || path.contains("netherite")) return "speculative_future";
        if (material.startsWith("SHULKER_") || result == Material.ELYTRA || path.contains("shulker")) return "space_age";
        if (result == Material.CRAFTER || path.contains("crafter")
                || result == Material.OBSERVER || path.contains("observer")
                || result == Material.CALIBRATED_SCULK_SENSOR) return "digital_age";
        if (isIndustrialOutput(result) || containsAny(path,
                "piston", "hopper", "dispenser", "dropper", "minecart", "rail")) return "industrial_revolution";
        if (isElectricalOutput(material) || containsAny(path,
                "redstone", "repeater", "comparator", "daylight_detector", "tripwire")) return "electrification";
        if (material.equals("BEACON") || material.equals("BREWING_STAND")
                || material.equals("ENDER_CHEST") || material.equals("CONDUIT")
                || path.contains("enchant")) return "mythic_age";
        if (material.startsWith("DIAMOND_") || path.contains("diamond")) return "renaissance";
        if (material.startsWith("IRON_") || material.startsWith("GOLD_")
                || material.startsWith("CHAINMAIL_") || material.equals("ANVIL")) return "medieval";
        if (material.startsWith("COPPER_") || material.contains("QUARTZ")
                || material.equals("AMETHYST_BLOCK") || material.equals("CALCITE")) return "modern";
        if (containsAny(material, "SCULK_SENSOR", "SCULK_CATALYST", "SCULK_SHRIEKER", "ECHO_SHARD")) return "information_age";
        return "ancient";
    }

    private static boolean isMythicOutput(String material, String path) {
        return containsAny(material, "ENCHANTING_TABLE", "BEACON", "BREWING_STAND",
                "ENDER_CHEST", "CONDUIT", "END_CRYSTAL", "END_ROD", "DRAGON_BREATH",
                "NETHER_STAR", "TOTEM_OF_UNDYING", "ENCHANTED_GOLDEN_APPLE")
                || containsAny(path, "enchant", "brewing_stand", "beacon", "ender_chest",
                "conduit", "end_crystal", "dragon_breath", "nether_star", "totem");
    }

    private static boolean isModernOutput(String material, String path) {
        return containsAny(material, "AMETHYST_BLOCK", "CALCITE", "COPPER_GRATE",
                "COPPER_DOOR", "COPPER_TRAPDOOR", "CUT_COPPER", "CHISELED_COPPER")
                || containsAny(path, "copper_grate", "copper_door", "copper_trapdoor",
                "cut_copper", "chiseled_copper", "amethyst_block", "calcite");
    }

    private static boolean isRenaissanceOutput(String material, String path) {
        return material.startsWith("DIAMOND_") || material.equals("DIAMOND")
                || material.equals("SPYGLASS") || material.equals("COMPASS")
                || material.equals("MAP") || material.equals("CARTOGRAPHY_TABLE")
                || material.contains("QUARTZ")
                || containsAny(path, "diamond", "spyglass", "compass", "map", "cartography", "quartz");
    }

    private static boolean isMedievalOutput(String material, String path) {
        return material.startsWith("IRON_") || material.startsWith("GOLD_")
                || material.startsWith("CHAINMAIL_")
                || containsAny(material, "ANVIL", "BUCKET", "CAULDRON", "SHEARS",
                "CLOCK", "LOOM", "GRINDSTONE", "STONECUTTER", "SMITHING_TABLE",
                "LECTERN", "CHAIN", "LANTERN", "BELL")
                || containsAny(path, "anvil", "bucket", "cauldron", "shears", "clock",
                "loom", "grindstone", "stonecutter", "smithing_table", "lectern", "lantern");
    }

    private static boolean isAncientOutput(String material, String path) {
        if (containsAny(material, "COPPER_INGOT", "RAW_COPPER", "COPPER_BLOCK",
                "OAK_PLANKS", "CRAFTING_TABLE", "COBBLESTONE", "STONE", "DIRT",
                "GRAVEL", "SAND", "GLASS", "GLASS_PANE", "BRICK", "BRICKS",
                "TERRACOTTA", "FLOWER_POT", "PAPER", "BOOK", "BREAD", "BOWL",
                "TORCH", "FURNACE", "CHEST", "BARREL", "STICK", "LADDER")) return true;

        if (material.startsWith("WOODEN_") || material.startsWith("STONE_")) return true;
        if (material.endsWith("_PLANKS") || material.endsWith("_LOG")
                || material.endsWith("_WOOD") || material.endsWith("_WOOL")
                || material.endsWith("_CARPET") || material.endsWith("_BED")
                || material.endsWith("_BOAT") || material.endsWith("_CHEST_BOAT")
                || material.endsWith("_FENCE") || material.endsWith("_FENCE_GATE")
                || material.endsWith("_SIGN") || material.endsWith("_HANGING_SIGN")
                || material.endsWith("_SLAB") || material.endsWith("_STAIRS")) return true;
        return containsAny(path, "planks", "wooden_", "stone_pickaxe", "stone_axe",
                "stone_shovel", "stone_hoe", "bread", "bowl", "torch", "paper",
                "glass", "brick", "terracotta", "fence", "boat", "carpet", "bed");
    }

    private static boolean isElectricalOutput(String material) {
        return containsAny(material, "REDSTONE_LAMP", "REPEATER", "COMPARATOR",
                "REDSTONE_TORCH", "REDSTONE_BLOCK", "DAYLIGHT_DETECTOR",
                "LIGHTNING_ROD", "COPPER_BULB");
    }

    private static String typeOf(Recipe recipe) {
        if (recipe instanceof ShapedRecipe) return "Moldada";
        if (recipe instanceof ShapelessRecipe) return "Sem formato";
        if (recipe instanceof SmithingRecipe) return "Ferraria";
        if (recipe instanceof StonecuttingRecipe) return "Corte de pedra";
        if (recipe instanceof BlastingRecipe) return "Alto-forno";
        if (recipe instanceof SmokingRecipe) return "Defumador";
        if (recipe instanceof FurnaceRecipe) return "Fornalha";
        if (recipe instanceof CookingRecipe<?>) return "Cozimento";
        return "Especial";
    }

    private static boolean isIndustrialOutput(Material result) {
        return result == Material.PISTON || result == Material.STICKY_PISTON
                || result == Material.HOPPER || result == Material.DISPENSER
                || result == Material.DROPPER || result == Material.MINECART
                || result == Material.CHEST_MINECART || result == Material.FURNACE_MINECART
                || result == Material.RAIL || result == Material.POWERED_RAIL
                || result == Material.DETECTOR_RAIL || result == Material.ACTIVATOR_RAIL
                || result == Material.BLAST_FURNACE || result == Material.SMOKER;
    }

    private static boolean isElectricalOutput(Material result) {
        return result == Material.REDSTONE_LAMP || result == Material.REPEATER
                || result == Material.COMPARATOR || result == Material.REDSTONE_TORCH
                || result == Material.REDSTONE_BLOCK || result == Material.DAYLIGHT_DETECTOR
                || result == Material.TARGET || result == Material.TRIPWIRE_HOOK;
    }

    private static boolean isCombatOutput(Material result) {
        String name = result.name();
        return name.endsWith("_SWORD")
                || name.equals("BOW")
                || name.equals("CROSSBOW")
                || name.equals("TRIDENT")
                || name.equals("MACE")
                || name.equals("SHIELD")
                || name.equals("ARROW")
                || name.equals("SPECTRAL_ARROW")
                || name.equals("TIPPED_ARROW");
    }

    private static boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) return true;
        }
        return false;
    }
}
