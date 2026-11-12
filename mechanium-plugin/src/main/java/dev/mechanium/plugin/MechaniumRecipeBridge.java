package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.event.MechaniumRecipeRegisterEvent;
import dev.mechanium.api.model.RecipeDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashSet;
import java.util.Set;

/** Converts validated MECHANIUM recipes to native Bukkit crafting recipes. */
final class MechaniumRecipeBridge implements Listener {
    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final Set<NamespacedKey> registered = new LinkedHashSet<>();

    MechaniumRecipeBridge(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
    }

    void registerAll() {
        for (RecipeDefinition recipe : api.getRecipes()) register(recipe);
    }

    @EventHandler
    public void onRecipeRegistered(MechaniumRecipeRegisterEvent event) {
        register(event.recipe());
    }

    void register(RecipeDefinition definition) {
        NamespacedKey key = new NamespacedKey(plugin, definition.id());
        if (registered.contains(key)) return;

        ItemStack result = api.createItemStack(definition.resultItemId(), definition.resultAmount())
                .orElseThrow(() -> new IllegalStateException("Missing result item " + definition.resultItemId()));

        Recipe recipe;
        if (definition.type() == RecipeDefinition.Type.SHAPED) {
            ShapedRecipe shaped = new ShapedRecipe(key, result);
            shaped.shape(definition.shape().toArray(String[]::new));
            definition.ingredients().forEach((symbol, value) -> shaped.setIngredient(symbol, choice(value)));
            recipe = shaped;
        } else {
            ShapelessRecipe shapeless = new ShapelessRecipe(key, result);
            definition.ingredients().values().forEach(value -> shapeless.addIngredient(choice(value)));
            recipe = shapeless;
        }

        if (!Bukkit.addRecipe(recipe)) {
            throw new IllegalStateException("Bukkit refused recipe " + definition.id());
        }
        registered.add(key);
    }

    /** Removes only recipes owned by this bridge, including partially completed startup. */
    void unregisterAll() {
        for (NamespacedKey key : registered) {
            Bukkit.removeRecipe(key);
        }
        registered.clear();
    }

    private RecipeChoice choice(String value) {
        if (value.startsWith("mechanium:")) {
            ItemStack item = api.createItemStack(value.substring(10), 1)
                    .orElseThrow(() -> new IllegalStateException("Unknown custom ingredient " + value));
            return new RecipeChoice.ExactChoice(item);
        }
        return new RecipeChoice.MaterialChoice(Material.valueOf(value));
    }
}
