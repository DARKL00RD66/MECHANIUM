package dev.mechanium.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Transfers MECHANIUM-tagged items through owner-matched industrial conduits. */
final class IndustrialNetworkManager implements Listener {
    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };
    private static final int MAX_NETWORK_SIZE = 128;

    private final JavaPlugin plugin;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey ownerKey;
    private BukkitTask task;

    IndustrialNetworkManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.itemIdKey = NamespacedKey.fromString("mechanium:item_id");
        this.ownerKey = new NamespacedKey(plugin, "industrial_network_owner");
    }

    void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::transferNetworks, 40L, 40L);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        String id = itemId(event.getItemInHand());
        if (!"industrial_conduit".equals(id) && !"network_crate".equals(id)) return;
        Material expected = "industrial_conduit".equals(id) ? Material.DISPENSER : Material.BARREL;
        Block block = event.getBlockPlaced();
        if (block.getType() != expected || !(block.getState() instanceof TileState tile)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cEste componente precisa ser colocado como " + expected.name() + ".");
            return;
        }
        tile.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, id);
        tile.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                event.getPlayer().getUniqueId().toString());
        if (!tile.update(true, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNão foi possível registrar o componente da rede.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        String id = tileItemId(event.getBlock());
        if (!isNetworkNode(id)) return;
        Block block = event.getBlock();
        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) return;
        String owner = tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (owner != null && !event.getPlayer().hasPermission("mechanium.admin")
                && !owner.equals(event.getPlayer().getUniqueId().toString())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cSomente o proprietário ou um administrador pode desmontar este componente.");
            return;
        }

        List<ItemStack> contents = new ArrayList<>();
        if ("network_crate".equals(id) && state instanceof Container container) {
            for (ItemStack item : container.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) contents.add(item.clone());
            }
        }
        ItemStack recovered = new ItemStack(block.getType());
        Location drop = block.getLocation().add(0.5, 0.5, 0.5);
        event.setDropItems(false);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || !block.getType().isAir()) return;
            for (ItemStack item : contents) block.getWorld().dropItemNaturally(drop, item);
            block.getWorld().dropItemNaturally(drop, recovered);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null
                || event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        String id = tileItemId(event.getClickedBlock());
        if ("industrial_conduit".equals(id)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§bConduíte industrial: §7conecte caixas do mesmo proprietário pelas faces.");
        } else if ("network_crate".equals(id)) {
            String owner = tileOwner(event.getClickedBlock());
            if (owner != null && !event.getPlayer().hasPermission("mechanium.admin")
                    && !owner.equals(event.getPlayer().getUniqueId().toString())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cEsta caixa pertence a outro jogador.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isNetworkNodeAt(event.getSource().getLocation())
                || isNetworkNodeAt(event.getDestination().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isNetworkNode);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isNetworkNode);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isNetworkNode)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isNetworkNode)) event.setCancelled(true);
    }

    private void transferNetworks() {
        Set<String> processed = new HashSet<>();
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof TileState tile)
                            || !"industrial_conduit".equals(tile.getPersistentDataContainer()
                            .get(itemIdKey, PersistentDataType.STRING))) continue;
                    String owner = tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
                    if (owner == null || processed.contains(key(state.getBlock()))) continue;
                    Network network = collectNetwork(state.getBlock(), owner);
                    if (network.pipes().isEmpty()) continue;
                    boolean overlap = network.pipes().stream().anyMatch(processed::contains);
                    processed.addAll(network.pipes());
                    if (!overlap && network.complete()) moveOneCustomItem(network.crates());
                }
            }
        }
    }

    private Network collectNetwork(Block firstPipe, String owner) {
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        List<Inventory> crates = new ArrayList<>();
        queue.add(firstPipe);
        while (!queue.isEmpty() && visited.size() < MAX_NETWORK_SIZE) {
            Block pipe = queue.removeFirst();
            if (!visited.add(key(pipe)) || !isPipe(pipe, owner)) continue;
            for (BlockFace face : FACES) {
                Block adjacent = adjacentLoaded(pipe, face);
                if (adjacent == null) continue;
                if (isPipe(adjacent, owner)) {
                    if (!visited.contains(key(adjacent))) queue.addLast(adjacent);
                } else if (isCrate(adjacent, owner)) {
                    BlockState state = adjacent.getState();
                    if (state instanceof Container container && !crates.contains(container.getInventory())) {
                        crates.add(container.getInventory());
                    }
                }
            }
        }
        return new Network(visited, crates, queue.isEmpty());
    }

    private void moveOneCustomItem(List<Inventory> crates) {
        if (crates.size() < 2) return;
        for (int sourceIndex = 0; sourceIndex < crates.size(); sourceIndex++) {
            Inventory source = crates.get(sourceIndex);
            for (int slot = 0; slot < source.getSize(); slot++) {
                ItemStack stack = source.getItem(slot);
                if (!isMechaniumItem(stack)) continue;
                for (int targetIndex = 0; targetIndex < crates.size(); targetIndex++) {
                    if (targetIndex == sourceIndex) continue;
                    Inventory target = crates.get(targetIndex);
                    ItemStack moved = stack.clone();
                    moved.setAmount(1);
                    if (!target.addItem(moved).isEmpty()) continue;
                    if (stack.getAmount() == 1) source.setItem(slot, null);
                    else {
                        stack.setAmount(stack.getAmount() - 1);
                        source.setItem(slot, stack);
                    }
                    return;
                }
            }
        }
    }

    private boolean isMechaniumItem(ItemStack item) {
        return item != null && !item.getType().isAir() && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer()
                .has(itemIdKey, PersistentDataType.STRING);
    }

    private boolean isPipe(Block block, String owner) {
        return "industrial_conduit".equals(tileItemId(block)) && owner.equals(tileOwner(block));
    }

    private boolean isCrate(Block block, String owner) {
        return block.getType() == Material.BARREL && "network_crate".equals(tileItemId(block))
                && owner.equals(tileOwner(block));
    }

    private boolean isNetworkNodeAt(Location location) {
        if (location == null || location.getWorld() == null
                || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return false;
        return isNetworkNode(location.getBlock());
    }

    private boolean isNetworkNode(Block block) {
        return isNetworkNode(tileItemId(block));
    }

    private boolean isNetworkNode(String id) {
        return "industrial_conduit".equals(id) || "network_crate".equals(id);
    }

    private String tileItemId(Block block) {
        if (block == null || !(block.getState() instanceof TileState tile)) return null;
        return tile.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    private String tileOwner(Block block) {
        if (block == null || !(block.getState() instanceof TileState tile)) return null;
        return tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
    }

    private String itemId(ItemStack item) {
        if (item == null || !item.hasItemMeta() || itemIdKey == null) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    private Block adjacentLoaded(Block block, BlockFace face) {
        int x = block.getX() + face.getModX();
        int y = block.getY() + face.getModY();
        int z = block.getZ() + face.getModZ();
        World world = block.getWorld();
        if (!world.isChunkLoaded(x >> 4, z >> 4)) return null;
        return world.getBlockAt(x, y, z);
    }

    private String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private record Network(Set<String> pipes, List<Inventory> crates, boolean complete) { }
}
