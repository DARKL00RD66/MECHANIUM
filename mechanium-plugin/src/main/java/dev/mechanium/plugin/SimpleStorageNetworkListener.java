package dev.mechanium.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Combined paginated inventory for face-connected simple-storage nodes owned by the same player. */
final class SimpleStorageNetworkListener implements Listener {
    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };
    private static final int PAGE_SIZE = 45;
    private static final int MAX_NODES = 64;

    private final NamespacedKey itemIdKey = NamespacedKey.fromString("mechanium:item_id");
    private final NamespacedKey ownerKey;
    private final NamespacedKey trustedKey;
    private final NamespacedKey storageTypeKey;
    private final Map<String, UUID> networkLocks = new HashMap<>();
    private final Map<UUID, StorageSession> sessions = new HashMap<>();

    SimpleStorageNetworkListener(JavaPlugin plugin) {
        this.ownerKey = new NamespacedKey(plugin, "private_storage_owner");
        this.trustedKey = new NamespacedKey(plugin, "private_storage_trusted");
        this.storageTypeKey = new NamespacedKey(plugin, "private_storage_type");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND
                || event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                || event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        String owner = ownerOf(block);
        if (owner == null || !isSimpleNode(block)) return;
        Player player = event.getPlayer();
        if (!canAccess(player, block, owner)) return;
        event.setCancelled(true);
        openNetwork(player, block, owner);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof NetworkHolder holder)) return;
        StorageSession session = holder.session;
        if (!(event.getWhoClicked() instanceof Player player)
                || !session.playerId.equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        int raw = event.getRawSlot();
        if (raw >= event.getView().getTopInventory().getSize() && event.isShiftClick()) {
            event.setCancelled(true);
            return;
        }
        if (raw < 0 || raw >= event.getView().getTopInventory().getSize()) return;
        if (raw == 45 && session.page > 0) {
            event.setCancelled(true);
            savePage(session);
            session.page--;
            render(session);
            player.updateInventory();
        } else if (raw == 53 && session.page + 1 < pageCount(session)) {
            event.setCancelled(true);
            savePage(session);
            session.page++;
            render(session);
            player.updateInventory();
        } else if (raw >= PAGE_SIZE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof NetworkHolder)) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot >= PAGE_SIZE && slot < 54)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof NetworkHolder holder)) return;
        StorageSession session = holder.session;
        savePage(session);
        sessions.remove(session.playerId);
        session.lockKeys.forEach(lockKey -> networkLocks.remove(lockKey, session.playerId));
    }

    void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (sessions.containsKey(player.getUniqueId())) player.closeInventory();
        }
        sessions.clear();
        networkLocks.clear();
    }

    private void openNetwork(Player player, Block start, String owner) {
        List<Block> nodes = collectNodes(start, owner, player);
        nodes.sort(Comparator.comparing(this::key));
        List<String> lockKeys = nodes.stream().map(this::key).toList();
        boolean locked = lockKeys.stream().map(networkLocks::get)
                .anyMatch(ownerId -> ownerId != null && !ownerId.equals(player.getUniqueId()));
        if (locked) {
            player.sendMessage("§cUm ou mais depósitos desta rede já estão abertos por outro jogador.");
            return;
        }
        if (sessions.containsKey(player.getUniqueId())) player.closeInventory();

        List<Inventory> inventories = new ArrayList<>();
        for (Block node : nodes) {
            BlockState state = node.getState();
            if (state instanceof Container container) inventories.add(container.getInventory());
        }
        if (inventories.isEmpty()) return;

        StorageSession session = new StorageSession(player.getUniqueId(), lockKeys, inventories);
        NetworkHolder holder = new NetworkHolder(session);
        Inventory inventory = Bukkit.createInventory(holder, 54,
                Component.text("MECHANIUM • Armazém conectado (" + inventories.size() + ")"));
        holder.inventory = inventory;
        session.inventory = inventory;
        sessions.put(player.getUniqueId(), session);
        lockKeys.forEach(lockKey -> networkLocks.put(lockKey, player.getUniqueId()));
        render(session);
        player.openInventory(inventory);
    }

    private List<Block> collectNodes(Block start, String owner, Player player) {
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        List<Block> result = new ArrayList<>();
        queue.add(start);
        while (!queue.isEmpty() && result.size() < MAX_NODES) {
            Block node = queue.removeFirst();
            if (!visited.add(key(node)) || !isSimpleNode(node) || !owner.equals(ownerOf(node))
                    || !canAccess(player, node, owner)) continue;
            result.add(node);
            for (BlockFace face : FACES) {
                int x = node.getX() + face.getModX();
                int y = node.getY() + face.getModY();
                int z = node.getZ() + face.getModZ();
                if (!node.getWorld().isChunkLoaded(x >> 4, z >> 4)) continue;
                Block adjacent = node.getWorld().getBlockAt(x, y, z);
                if (isSimpleNode(adjacent) && !visited.contains(key(adjacent))) queue.addLast(adjacent);
            }
        }
        return result;
    }

    private void render(StorageSession session) {
        session.inventory.clear();
        int first = session.page * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int index = first + slot;
            if (index >= session.inventories.size() * 27) break;
            Inventory source = session.inventories.get(index / 27);
            ItemStack item = source.getItem(index % 27);
            if (item != null && !item.getType().isAir()) session.inventory.setItem(slot, item.clone());
        }
        if (session.page > 0) session.inventory.setItem(45, navigationItem(Material.ARROW, "Página anterior"));
        session.inventory.setItem(49, navigationItem(Material.BARREL,
                "Rede • " + session.inventories.size() + " nós • página " + (session.page + 1) + "/" + pageCount(session)));
        if (session.page + 1 < pageCount(session)) session.inventory.setItem(53, navigationItem(Material.ARROW, "Próxima página"));
    }

    private void savePage(StorageSession session) {
        int first = session.page * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int index = first + slot;
            if (index >= session.inventories.size() * 27) break;
            Inventory target = session.inventories.get(index / 27);
            ItemStack item = session.inventory.getItem(slot);
            target.setItem(index % 27, item == null || item.getType().isAir() ? null : item.clone());
        }
    }

    private int pageCount(StorageSession session) {
        return Math.max(1, (session.inventories.size() * 27 + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private ItemStack navigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    private boolean isSimpleNode(Block block) {
        if (block == null || block.getType() != Material.BARREL || !(block.getState() instanceof TileState tile)) return false;
        String type = tile.getPersistentDataContainer().get(storageTypeKey, PersistentDataType.STRING);
        return "simple_storage_node".equals(type) || "network_storage_hub".equals(type);
    }

    private String ownerOf(Block block) {
        if (block == null || !(block.getState() instanceof TileState tile)) return null;
        return tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
    }

    private boolean canAccess(Player player, Block block, String owner) {
        if (player.hasPermission("mechanium.admin") || player.getUniqueId().toString().equals(owner)) return true;
        if (!(block.getState() instanceof TileState tile)) return false;
        String raw = tile.getPersistentDataContainer().get(trustedKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return false;
        for (String uuid : raw.split(",")) {
            if (player.getUniqueId().toString().equals(uuid.trim())) return true;
        }
        return false;
    }

    private String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private static final class NetworkHolder implements InventoryHolder {
        private final StorageSession session;
        private Inventory inventory;

        private NetworkHolder(StorageSession session) { this.session = session; }
        @Override public Inventory getInventory() { return inventory; }
    }

    private static final class StorageSession {
        private final UUID playerId;
        private final List<String> lockKeys;
        private final List<Inventory> inventories;
        private int page;
        private Inventory inventory;

        private StorageSession(UUID playerId, List<String> lockKeys, List<Inventory> inventories) {
            this.playerId = playerId;
            this.lockKeys = List.copyOf(lockKeys);
            this.inventories = inventories;
        }
    }
}
