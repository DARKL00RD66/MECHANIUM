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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Owner-protected barrel storage. Ownership and trusted users are stored on the block entity,
 * so access rules survive restarts and chunk unloads.
 */
final class PrivateStorageListener implements Listener {
    private static final String STORAGE_ITEM_ID = "private_storage_crate";
    private static final int MAX_TRUSTED_PLAYERS = 32;

    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey trustedKey;
    private final NamespacedKey storageTypeKey;

    PrivateStorageListener(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
        this.itemIdKey = new NamespacedKey("mechanium", "item_id");
        this.ownerKey = new NamespacedKey(plugin, "private_storage_owner");
        this.trustedKey = new NamespacedKey(plugin, "private_storage_trusted");
        this.storageTypeKey = new NamespacedKey(plugin, "private_storage_type");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        String storageItemId = storageItemId(event.getItemInHand());
        if (storageItemId == null) return;

        BlockState state = event.getBlockPlaced().getState();
        if (!(state instanceof TileState tile) || event.getBlockPlaced().getType() != Material.BARREL) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Este módulo exige um barril compatível.");
            return;
        }

        tile.getPersistentDataContainer().set(storageTypeKey, PersistentDataType.STRING, storageItemId);
        tile.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                event.getPlayer().getUniqueId().toString());
        tile.getPersistentDataContainer().set(trustedKey, PersistentDataType.STRING, "");
        if (!tile.update(true, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Não foi possível gravar o proprietário do armazém.");
            return;
        }
        event.getPlayer().sendMessage(ChatColor.GREEN + "Armazém privado instalado. Apenas você e jogadores autorizados podem acessá-lo.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Optional<String> owner = ownerOf(event.getClickedBlock());
        if (owner.isEmpty()) return;
        if (canAccess(event.getPlayer(), event.getClickedBlock(), owner.get())) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "Este armazém pertence a outro jogador.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Optional<String> owner = ownerOf(event.getBlock());
        if (owner.isEmpty()) return;

        Player player = event.getPlayer();
        if (!canManage(player, owner.get())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Somente o proprietário ou um administrador pode destruir este armazém.");
            return;
        }

        Block block = event.getBlock();
        List<ItemStack> contents = new ArrayList<>();
        BlockState state = block.getState();
        if (state instanceof Container container) {
            for (ItemStack content : container.getInventory().getContents()) {
                if (content != null && !content.getType().isAir()) contents.add(content.clone());
            }
        }

        event.setDropItems(false);
        String storageItemId = STORAGE_ITEM_ID;
        if (state instanceof TileState tile) {
            storageItemId = tile.getPersistentDataContainer().getOrDefault(
                    storageTypeKey, PersistentDataType.STRING, STORAGE_ITEM_ID);
        }
        ItemStack recovered = api.createItemStack(storageItemId, 1)
                .orElseGet(() -> new ItemStack(Material.BARREL));
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        // Defer world drops until Bukkit has finalized the break event. Another plugin may cancel it later.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || !block.getType().isAir()) return;
            for (ItemStack content : contents) {
                block.getWorld().dropItemNaturally(dropLocation, content);
            }
            block.getWorld().dropItemNaturally(dropLocation, recovered);
        });
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

    boolean canManageStorage(Player player, Block block) {
        Optional<String> owner = ownerOf(block);
        return owner.isPresent() && canManage(player, owner.get());
    }

    boolean changeTrustedPlayer(Player actor, Block block, UUID targetId, boolean trust) {
        if (!canManageStorage(actor, block) || targetId == null) return false;
        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) return false;

        Set<String> trusted = trustedIds(tile);
        String target = targetId.toString();
        boolean changed = trust ? trusted.add(target) : trusted.remove(target);
        if (!changed || trusted.size() > MAX_TRUSTED_PLAYERS) return false;

        tile.getPersistentDataContainer().set(trustedKey, PersistentDataType.STRING,
                String.join(",", trusted));
        return tile.update(true, false);
    }

    List<UUID> trustedPlayers(Player actor, Block block) {
        if (!canManageStorage(actor, block)) return List.of();
        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) return List.of();
        List<UUID> result = new ArrayList<>();
        for (String raw : trustedIds(tile)) {
            try {
                result.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("UUID inválido na lista de acesso de um armazém privado.");
            }
        }
        return List.copyOf(result);
    }

    private String storageItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String id = item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING);
        return STORAGE_ITEM_ID.equals(id) || "simple_storage_node".equals(id)
                || "network_storage_node".equals(id) ? id : null;
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

    private Set<String> trustedIds(TileState tile) {
        String raw = tile.getPersistentDataContainer().get(trustedKey, PersistentDataType.STRING);
        Set<String> trusted = new LinkedHashSet<>();
        if (raw == null || raw.isBlank()) return trusted;
        for (String value : raw.split(",")) {
            if (!value.isBlank()) trusted.add(value.trim());
        }
        return trusted;
    }

    private boolean canAccess(Player player, Block block, String owner) {
        return PrivateStorageAccessPolicy.canAccess(player.getUniqueId(), owner,
                trustedIds((TileState) block.getState()), player.hasPermission("mechanium.admin"));
    }

    private boolean canManage(Player player, String owner) {
        return PrivateStorageAccessPolicy.canManage(
                player.getUniqueId(), owner, player.hasPermission("mechanium.admin"));
    }
}
