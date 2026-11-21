package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.EraDefinition;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.Keyed;

/**
 * Applies administrator-configured historical era requirements to selected vanilla crafting recipes.
 * Recipes not present in the configuration are left untouched.
 */
final class VanillaRecipeGateListener implements Listener {
    private final MechaniumPlugin plugin;
    private final MechaniumApi api;

    VanillaRecipeGateListener(MechaniumPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVanillaCraft(CraftItemEvent event) {
        if (!plugin.getConfig().getBoolean("vanilla-recipe-gates.enabled", true)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getRecipe() instanceof Keyed keyed)) return;

        NamespacedKey key = keyed.getKey();
        if (!key.getNamespace().equals("minecraft")) return;

        String requiredEraId = plugin.getConfig().getString(
                "vanilla-recipe-gates.recipes." + key, null);
        if (requiredEraId == null || requiredEraId.isBlank()
                || requiredEraId.equalsIgnoreCase("off")) return;

        EraDefinition requiredEra = api.getEra(requiredEraId).orElse(null);
        EraDefinition currentEra = api.getPlayerEra(player.getUniqueId())
                .flatMap(api::getEra).orElse(null);

        // Invalid configuration must not silently grant access to a gated recipe.
        if (requiredEra == null) {
            event.setCancelled(true);
            plugin.getLogger().warning("Receita vanilla " + key
                    + " aponta para uma era inexistente: " + requiredEraId);
            player.sendMessage(ChatColor.RED + "Esta receita está configurada com uma era inválida. Avise a administração.");
            return;
        }
        if (currentEra == null || currentEra.order() < requiredEra.order()) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Esta receita exige a era "
                    + ChatColor.YELLOW + requiredEra.displayName() + ChatColor.RED + ".");
        }
    }
}
