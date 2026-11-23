package dev.mechanium.plugin;

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.EraDefinition;
import io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent;
import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.PlayerRecipeDiscoverEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.SmithingInventory;

/**
 * Enforces historical requirements for vanilla recipes discovered from Paper's recipe registry.
 * Player-driven work uses the player's effective era; autonomous blocks use the global server era.
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
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getRecipe() instanceof Keyed keyed)) return;
        enforce(event, keyed.getKey(), player, "crafting");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (!(event.getRecipe() instanceof Keyed keyed)) return;
        enforce(event, keyed.getKey(), null, "crafter automático");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof Keyed keyed)) return;
        enforce(event, keyed.getKey(), null, "processamento em fornalha");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSmithing(SmithItemEvent event) {
        if (!(event.getView().getTopInventory() instanceof SmithingInventory inventory)) return;
        Recipe recipe = inventory.getRecipe();
        if (!(recipe instanceof Keyed keyed)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        enforce(event, keyed.getKey(), player, "ferraria");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onStonecutterSelect(PlayerStonecutterRecipeSelectEvent event) {
        enforce(event, event.getStonecuttingRecipe().getKey(), event.getPlayer(), "corte de pedra");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRecipeBookClick(PlayerRecipeBookClickEvent event) {
        enforce(event, event.getRecipe(), event.getPlayer(), "livro de receitas");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRecipeDiscover(PlayerRecipeDiscoverEvent event) {
        if (!isGateSystemEnabled()) return;
        NamespacedKey key = event.getRecipe();
        if (!"minecraft".equals(key.getNamespace())) return;
        String requiredEraId = requiredEra(key);
        if (requiredEraId == null) return;

        EraDefinition requiredEra = resolveRequiredEra(key, requiredEraId);
        EraDefinition currentEra = api.getPlayerEra(event.getPlayer().getUniqueId())
                .flatMap(api::getEra).orElse(null);
        if (requiredEra == null || currentEra == null
                || currentEra.order() < requiredEra.order()) {
            event.setCancelled(true);
        }
    }

    private void enforce(Cancellable event, NamespacedKey key, Player player, String actionName) {
        if (!isGateSystemEnabled()) return;
        if (!"minecraft".equals(key.getNamespace())) return;

        String requiredEraId = requiredEra(key);
        if (requiredEraId == null) return;

        EraDefinition requiredEra = resolveRequiredEra(key, requiredEraId);
        EraDefinition currentEra;
        if (player == null) {
            currentEra = api.getServerEra().flatMap(api::getEra).orElse(null);
        } else {
            currentEra = api.getPlayerEra(player.getUniqueId()).flatMap(api::getEra).orElse(null);
        }

        if (requiredEra == null) {
            event.setCancelled(true);
            plugin.getLogger().warning("Receita vanilla " + key
                    + " aponta para uma era inexistente: " + requiredEraId);
            if (player != null) {
                player.sendMessage(ChatColor.RED
                        + "Esta receita está configurada com uma era inválida. Avise a administração.");
            }
            return;
        }

        if (currentEra == null || currentEra.order() < requiredEra.order()) {
            event.setCancelled(true);
            if (player != null) {
                player.sendMessage(ChatColor.RED + "Esta receita exige a era "
                        + ChatColor.YELLOW + requiredEra.displayName() + ChatColor.RED + ".");
            } else {
                plugin.getLogger().fine("Receita " + key + " bloqueada em " + actionName
                        + " até a era global " + requiredEra.id() + ".");
            }
        }
    }

    private boolean isGateSystemEnabled() {
        return plugin.getConfig().getBoolean("vanilla-recipe-gates.enabled", true);
    }

    private String requiredEra(NamespacedKey key) {
        String value = plugin.getConfig().getString(
                "vanilla-recipe-gates.recipes." + key, null);
        if (value == null || value.isBlank() || value.equalsIgnoreCase("off")) return null;
        return value;
    }

    private EraDefinition resolveRequiredEra(NamespacedKey key, String requiredEraId) {
        EraDefinition era = api.getEra(requiredEraId).orElse(null);
        if (era == null) {
            plugin.getLogger().warning("Receita vanilla " + key
                    + " aponta para uma era inexistente: " + requiredEraId);
        }
        return era;
    }
}
