package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
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
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

/** Transfers MECHANIUM purified-water items between owner-matched tanks through connected hopper pipes. */
final class FluidPipeManager implements Listener {
    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };
    private static final int MAX_PIPE_NETWORK = 128;
    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey ownerKey;
    private final Map<String, Location> loadedPipes = new HashMap<>();
    private BukkitTask task;

    FluidPipeManager(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
        this.itemIdKey = NamespacedKey.fromString("mechanium:item_id");
        this.ownerKey = new NamespacedKey(plugin, "fluid_network_owner");
    }

    void start() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) registerChunkPipes(chunk);
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::transferNetworks, 40L, 40L);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        loadedPipes.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        String id = itemId(event.getItemInHand());
        if (!"fluid_pipe".equals(id) && !"liquid_tank".equals(id)) return;
        Material expected = "fluid_pipe".equals(id) ? Material.DISPENSER : Material.BARREL;
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
            event.getPlayer().sendMessage("§cNão foi possível registrar o componente de fluidos.");
            return;
        }
        if ("fluid_pipe".equals(id)) loadedPipes.put(key(block.getLocation()), block.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        String id = tileItemId(event.getBlock());
        if (!"fluid_pipe".equals(id) && !"liquid_tank".equals(id)) return;
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
        if (state instanceof Container container && "liquid_tank".equals(id)) {
            for (ItemStack item : container.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) contents.add(item.clone());
            }
        }
        ItemStack recovered = api.createItemStack(id, 1)
                .orElseGet(() -> new ItemStack(block.getType()));
        Location drop = block.getLocation().add(0.5, 0.5, 0.5);
        event.setDropItems(false);
        String locationKey = key(block.getLocation());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || !block.getType().isAir()) return;
            loadedPipes.remove(locationKey);
            for (ItemStack item : contents) block.getWorld().dropItemNaturally(drop, item);
            block.getWorld().dropItemNaturally(drop, recovered);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPipeInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null
                || event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        String id = tileItemId(event.getClickedBlock());
        if ("fluid_pipe".equals(id)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§bTubulação MECHANIUM: §7conecte tanques pelas faces para transportar água purificada.");
            return;
        }
        if ("liquid_tank".equals(id)) {
            String owner = tileOwner(event.getClickedBlock());
            if (owner != null && !event.getPlayer().hasPermission("mechanium.admin")
                    && !owner.equals(event.getPlayer().getUniqueId().toString())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cEste tanque pertence a outro jogador.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isTransportNodeAt(event.getSource().getLocation()) || isTransportNodeAt(event.getDestination().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isPipeOrTank);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isPipeOrTank);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isPipeOrTank)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isPipeOrTank)) event.setCancelled(true);
    }

    private void transferNetworks() {
        Set<String> processed = new HashSet<>();
        for (Location location : new ArrayList<>(loadedPipes.values())) {
            World world = location.getWorld();
            if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                loadedPipes.remove(key(location));
                continue;
            }
            BlockState state = location.getBlock().getState();
            if (!(state instanceof TileState tile)
                    || !"fluid_pipe".equals(tile.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING))) {
                loadedPipes.remove(key(location));
                continue;
            }
            String owner = tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (owner == null || processed.contains(key(state.getBlock()))) continue;
            Network network = collectNetwork(state.getBlock(), owner);
            if (network.pipes().isEmpty()) continue;
            boolean overlapsProcessedNetwork = network.pipes().stream().anyMatch(processed::contains);
            processed.addAll(network.pipes());
            if (!overlapsProcessedNetwork && network.complete()) moveOnePurifiedWater(network.tanks());
        }
    }

    private void registerChunkPipes(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof TileState tile)) continue;
            if ("fluid_pipe".equals(tile.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING))) {
                loadedPipes.put(key(state.getLocation()), state.getLocation());
            }
        }
    }

    @EventHandler
    public void onFluidChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        registerChunkPipes(event.getChunk());
    }

    @EventHandler
    public void onFluidChunkUnload(org.bukkit.event.world.ChunkUnloadEvent event) {
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        UUID worldId = event.getWorld().getUID();
        loadedPipes.entrySet().removeIf(entry -> {
            Location location = entry.getValue();
            return location.getWorld() != null && location.getWorld().getUID().equals(worldId)
                    && (location.getBlockX() >> 4) == chunkX && (location.getBlockZ() >> 4) == chunkZ;
        });
    }

    private Network collectNetwork(Block firstPipe, String owner) {
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        List<Inventory> tanks = new ArrayList<>();
        queue.add(firstPipe);
        while (!queue.isEmpty() && visited.size() < MAX_PIPE_NETWORK) {
            Block pipe = queue.removeFirst();
            if (!visited.add(key(pipe)) || !isPipe(pipe, owner)) continue;
            for (BlockFace face : FACES) {
                Block adjacent = adjacentLoaded(pipe, face);
                if (adjacent == null) continue;
                if (isPipe(adjacent, owner)) {
                    if (!visited.contains(key(adjacent))) queue.addLast(adjacent);
                } else if (isTank(adjacent, owner)) {
                    BlockState state = adjacent.getState();
                    if (state instanceof Container container && !tanks.contains(container.getInventory())) {
                        tanks.add(container.getInventory());
                    }
                }
            }
        }
        return new Network(visited, tanks, queue.isEmpty());
    }

    private void moveOnePurifiedWater(List<Inventory> tanks) {
        if (tanks.size() < 2) return;
        for (int sourceIndex = 0; sourceIndex < tanks.size(); sourceIndex++) {
            Inventory source = tanks.get(sourceIndex);
            for (int slot = 0; slot < source.getSize(); slot++) {
                ItemStack stack = source.getItem(slot);
                if (!isPurifiedWater(stack)) continue;
                for (int targetIndex = 0; targetIndex < tanks.size(); targetIndex++) {
                    if (targetIndex == sourceIndex) continue;
                    Inventory target = tanks.get(targetIndex);
                    int targetSlot = firstCompatibleSlot(target, stack);
                    if (targetSlot < 0) continue;
                    ItemStack existing = target.getItem(targetSlot);
                    if (existing == null || existing.getType().isAir()) {
                        ItemStack moved = stack.clone();
                        moved.setAmount(1);
                        target.setItem(targetSlot, moved);
                    } else {
                        existing.setAmount(existing.getAmount() + 1);
                        target.setItem(targetSlot, existing);
                    }
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

    private int firstCompatibleSlot(Inventory inventory, ItemStack item) {
        int empty = -1;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType().isAir()) {
                if (empty < 0) empty = slot;
            } else if (current.isSimilar(item) && current.getAmount() < current.getMaxStackSize()) {
                return slot;
            }
        }
        return empty;
    }

    private boolean isPurifiedWater(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return "purified_water".equals(item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING));
    }

    private boolean isPipe(Block block, String owner) {
        return "fluid_pipe".equals(tileItemId(block)) && owner.equals(tileOwner(block));
    }

    private boolean isTank(Block block, String owner) {
        return block.getType() == Material.BARREL && "liquid_tank".equals(tileItemId(block))
                && owner.equals(tileOwner(block));
    }

    private boolean isTransportNodeAt(Location location) {
        if (location == null || location.getWorld() == null
                || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return false;
        String id = tileItemId(location.getBlock());
        return "fluid_pipe".equals(id) || "liquid_tank".equals(id);
    }

    private boolean isPipeOrTank(Block block) {
        String id = tileItemId(block);
        return "fluid_pipe".equals(id) || "liquid_tank".equals(id);
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

    private boolean isLoaded(Location location) {
        return location.getWorld() != null
                && location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    private String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private String key(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":"
                + location.getBlockY() + ":" + location.getBlockZ();
    }

    private record Network(Set<String> pipes, List<Inventory> tanks, boolean complete) { }
}
