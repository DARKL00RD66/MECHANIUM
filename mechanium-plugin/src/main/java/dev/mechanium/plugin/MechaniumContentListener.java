package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Keyed;
import org.bukkit.persistence.PersistentDataType;

/** Enforces research gates when players use or craft registered MECHANIUM content. */
final class MechaniumContentListener implements Listener {
    private static final NamespacedKey ITEM_ID = new NamespacedKey("mechanium", "item_id");
    private final MechaniumApi api;

    MechaniumContentListener(MechaniumApi api) { this.api = api; }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) { enforce(event.getPlayer(), event.getItem(), event); }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) { enforce(event.getPlayer(), event.getItem(), event); }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) { enforce(event.getPlayer(), event.getItemInHand(), event); }

    @EventHandler(ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getRecipe() instanceof Keyed keyed)) return;
        if (!keyed.getKey().getNamespace().equals("mechanium")) return;
        String recipeId = keyed.getKey().getKey();
        if (!api.canCraft(player.getUniqueId(), recipeId)) {
            event.setCancelled(true);
            player.sendMessage("§cVocê ainda não desbloqueou a tecnologia necessária para fabricar este item.");
        }
    }

    private void enforce(Player player, ItemStack stack, org.bukkit.event.Cancellable event) {
        if (stack == null || !stack.hasItemMeta()) return;
        String itemId = stack.getItemMeta().getPersistentDataContainer().get(ITEM_ID, PersistentDataType.STRING);
        if (itemId == null || api.canUseItem(player.getUniqueId(), itemId)) return;
        event.setCancelled(true);
        player.sendMessage("§cVocê ainda não desbloqueou a tecnologia necessária para utilizar este item.");
    }
}
