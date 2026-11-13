package dev.mechanium.plugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/** Active utility abilities for reinforced gear and the field/maintenance kits. */
final class SpecialEquipmentListener implements Listener {
    private final NamespacedKey itemIdKey = NamespacedKey.fromString("mechanium:item_id");
    private final JavaPlugin plugin;

    SpecialEquipmentListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onReinforcedToolBreak(BlockBreakEvent event) {
        if (!"reinforced_pickaxe".equals(itemId(event.getPlayer().getInventory().getItemInMainHand()))) return;
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 30, 0, true, false, true));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND
                || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) return;
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        String id = itemId(held);
        if (id == null) return;

        if (id.equals("surveying_compass")) {
            event.setCancelled(true);
            Block target = player.getTargetBlockExact(32);
            String targetText = target == null ? "nenhum bloco" : target.getX() + ", " + target.getY() + ", " + target.getZ();
            player.sendMessage("§bLevantamento §7| §f" + player.getWorld().getName()
                    + " §7| §fX " + player.getLocation().getBlockX()
                    + " Y " + player.getLocation().getBlockY()
                    + " Z " + player.getLocation().getBlockZ()
                    + " §7| §f" + player.getLocation().getBlock().getBiome().name());
            player.sendMessage("§7Direção: §f" + player.getFacing().name()
                    + " §7| Bloco observado: §f" + targetText);
            return;
        }

        if (id.equals("field_toolkit")) {
            event.setCancelled(true);
            if (player.hasCooldown(held.getType())) return;
            player.setCooldown(held.getType(), 20 * 30);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 12, 0, true, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 30, 0, true, false, true));
            player.sendMessage("§eKit de campo ativado: mobilidade e visão noturna temporárias.");
            return;
        }

        if (id.equals("maintenance_kit")) {
            event.setCancelled(true);
            if (player.hasCooldown(held.getType())) return;
            if (repairOneItem(player)) {
                consumeOne(player, held);
                player.setCooldown(Material.ANVIL, 20 * 10);
                player.sendMessage("§aManutenção concluída: um equipamento foi reparado.");
            } else {
                player.sendMessage("§cNão há equipamento danificado para reparar no inventário.");
            }
        }
    }

    private boolean repairOneItem(Player player) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().isAir() || item.getType().getMaxDurability() <= 0) continue;
            if (item == player.getInventory().getItemInMainHand()) continue;
            ItemMeta meta = item.getItemMeta();
            if (!(meta instanceof Damageable damageable) || damageable.getDamage() <= 0) continue;
            int repair = Math.max(1, item.getType().getMaxDurability() / 4);
            damageable.setDamage(Math.max(0, damageable.getDamage() - repair));
            item.setItemMeta(meta);
            return true;
        }
        return false;
    }

    private void consumeOne(Player player, ItemStack held) {
        if (held.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else held.setAmount(held.getAmount() - 1);
    }

    private String itemId(ItemStack item) {
        if (item == null || !item.hasItemMeta() || itemIdKey == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }
}
