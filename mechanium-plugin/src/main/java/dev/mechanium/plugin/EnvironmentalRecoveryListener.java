package dev.mechanium.plugin;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Converts a crafted consumable into a reduction of the abstract in-game emissions counter. */
final class EnvironmentalRecoveryListener implements Listener {
    private static final String ITEM_ID = "carbon_capture_cell";
    private static final long RECOVERY_PER_CELL = 10L;

    private final NamespacedKey itemIdKey = new NamespacedKey("mechanium", "item_id");
    private final NamespacedKey emissionsKey;

    EnvironmentalRecoveryListener(JavaPlugin plugin) {
        this.emissionsKey = new NamespacedKey(plugin, "industrial_emissions");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || !held.hasItemMeta()) return;
        String id = held.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);
        if (!ITEM_ID.equals(id)) return;

        event.setCancelled(true);
        long current = player.getWorld().getPersistentDataContainer()
                .getOrDefault(emissionsKey, PersistentDataType.LONG, 0L);
        if (current <= 0L) {
            player.sendMessage("§7Este mundo não possui impacto industrial acumulado para recuperar.");
            return;
        }

        long recovered = Math.min(current, RECOVERY_PER_CELL);
        player.getWorld().getPersistentDataContainer().set(emissionsKey,
                PersistentDataType.LONG, current - recovered);
        if (held.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else held.setAmount(held.getAmount() - 1);
        player.sendMessage("§aCélula aplicada: §f-" + recovered
                + " §aunidades de impacto industrial. Restante: §f" + (current - recovered) + "§a.");
    }
}
