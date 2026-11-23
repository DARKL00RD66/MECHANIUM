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

        // Enchanting is deliberately mystical, not medieval or renaissance.
        if (result == Material.ENCHANTING_TABLE || path.equals("enchanting_table")) {
            return "mythic_age";
        }

        if (material.startsWith("NETHERITE_") || path.contains("netherite")) {
            return "speculative_future";
        }

        if (material.startsWith("SHULKER_") || result == Material.ELYTRA
                || path.contains("shulker")) {
            return "space_age";
        }

        if (result == Material.CRAFTER || path.contains("crafter")
                || result == Material.OBSERVER || path.contains("observer")
                || result == Material.CALIBRATED_SCULK_SENSOR) {
            return "digital_age";
        }

        if (isIndustrialOutput(result) || containsAny(path,
                "piston", "hopper", "dispenser", "dropper", "minecart", "rail")) {
            return "industrial_revolution";
        }

        if (isElectricalOutput(result) || containsAny(path,
                "redstone", "repeater", "comparator", "daylight_detector", "tripwire")) {
            return "electrification";
        }

        if (result == Material.BEACON || result == Material.BREWING_STAND
                || result == Material.ENDER_CHEST || result == Material.CONDUIT
                || path.contains("enchant")) {
            return "mythic_age";
        }

        if (material.startsWith("DIAMOND_") || path.contains("diamond")) {
            return "renaissance";
        }

        if (material.startsWith("IRON_") || material.startsWith("GOLD_")
                || material.startsWith("CHAINMAIL_") || result == Material.ANVIL) {
            return "medieval";
        }

        if (material.startsWith("COPPER_") || material.contains("QUARTZ")
                || result == Material.AMETHYST_BLOCK || result == Material.CALCITE) {
            return "modern";
        }

        if (result == Material.SCULK_SENSOR || result == Material.SCULK_CATALYST
                || result == Material.SCULK_SHRIEKER || result == Material.ECHO_SHARD) {
            return "information_age";
        }

        return "ancient";
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
