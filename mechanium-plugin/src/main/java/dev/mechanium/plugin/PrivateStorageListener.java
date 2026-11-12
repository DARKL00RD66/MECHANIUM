package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Owner-protected barrel storage. Ownership is stored on the placed block entity,
 * not in memory, so it survives restarts and chunk unloads.
 */
final class PrivateStorageListener implements Listener {
    private static final String STORAGE_ITEM_ID = "private_storage_crate";

    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey ownerKey;

    PrivateStorageListener(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
        this.itemIdKey = new NamespacedKey("mechanium", "item_id");
        this.ownerKey = new NamespacedKey(plugin, "private_storage_owner");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!isStorageItem(event.getItemInHand())) return;

        BlockState state = event.getBlockPlaced().getState();
        if (!(state instanceof TileState tile) || event.getBlockPlaced().getType() != Material.BARREL) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Este módulo exige um barril compatível.");
            return;
        }

        tile.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                event.getPlayer().getUniqueId().toString());
        if (!tile.update(true, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Não foi possível gravar o proprietário do armazém.");
            return;
        }
        event.getPlayer().sendMessage(ChatColor.GREEN + "Armazém privado instalado. Apenas você e administradores podem acessá-lo.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Optional<String> owner = ownerOf(event.getClickedBlock());
        if (owner.isEmpty()) return;
        if (canAccess(event.getPlayer(), owner.get())) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "Este armazém pertence a outro jogador.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Optional<String> owner = ownerOf(event.getBlock());
        if (owner.isEmpty()) return;

        Player player = event.getPlayer();
        if (!canAccess(player, owner.get())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Você não pode destruir o armazém de outro jogador.");
            return;
        }

        BlockState state = event.getBlock().getState();
        if (state instanceof Container container) {
            for (ItemStack content : container.getInventory().getContents()) {
                if (content != null && !content.getType().isAir()) {
                    event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), content.clone());
                }
            }
        }

        event.setDropItems(false);
        ItemStack recovered = api.createItemStack(STORAGE_ITEM_ID, 1)
                .orElseGet(() -> new ItemStack(Material.BARREL));
        Location dropLocation = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        event.getBlock().getWorld().dropItemNaturally(dropLocation, recovered);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isPrivateStorage);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isPrivateStorage);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isPrivateStorage)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isPrivateStorage)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isPrivateStorageInventory(event.getSource().getLocation())
                || isPrivateStorageInventory(event.getDestination().getLocation())) {
            event.setCancelled(true);
        }
    }

    private boolean isStorageItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);
        return STORAGE_ITEM_ID.equals(id);
    }

    private boolean isPrivateStorage(Block block) {
        return ownerOf(block).isPresent();
    }

    private boolean isPrivateStorageInventory(Location location) {
        return location != null && isPrivateStorage(location.getBlock());
    }

    private Optional<String> ownerOf(Block block) {
        if (block == null || block.getType() != Material.BARREL) return Optional.empty();
        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) return Optional.empty();
        // A malformed owner value must remain protected; it must never turn into an unclaimed barrel.
        return Optional.ofNullable(tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING));
    }

    private boolean canAccess(Player player, String owner) {
        return player.getUniqueId().toString().equals(owner) || player.hasPermission("mechanium.admin");
    }
}
