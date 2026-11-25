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
import org.bukkit.entity.Player;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

/** Transfers MECHANIUM-tagged items through owner-matched industrial conduits. */
final class IndustrialNetworkManager implements Listener {
    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST,
            BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };
    private static final int MAX_NETWORK_SIZE = 128;

    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey filterItemsKey;
    private final Map<String, Location> loadedPipes = new HashMap<>();
    private final Map<String, IndustrialNetworkCursor> transferCursors = new HashMap<>();
    private BukkitTask task;

    IndustrialNetworkManager(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
        this.itemIdKey = NamespacedKey.fromString("mechanium:item_id");
        this.ownerKey = new NamespacedKey(plugin, "industrial_network_owner");
        this.filterItemsKey = new NamespacedKey(plugin, "industrial_network_filter_items");
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
        transferCursors.clear();
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
        if ("network_crate".equals(id) && event.getItemInHand().hasItemMeta()) {
            String savedFilter = event.getItemInHand().getItemMeta().getPersistentDataContainer()
                    .get(filterItemsKey, PersistentDataType.STRING);
            if (savedFilter != null && !savedFilter.isBlank()) {
                tile.getPersistentDataContainer().set(filterItemsKey, PersistentDataType.STRING, savedFilter);
            }
        }
        if (!tile.update(true, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNão foi possível registrar o componente da rede.");
            return;
        }
        if ("industrial_conduit".equals(id)) loadedPipes.put(key(block.getLocation()), block.getLocation());
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
        ItemStack recovered = api.createItemStack(id, 1).orElseGet(() -> new ItemStack(block.getType()));
        if ("network_crate".equals(id)) {
            String savedFilter = tile.getPersistentDataContainer().get(filterItemsKey, PersistentDataType.STRING);
            if (savedFilter != null && !savedFilter.isBlank()) {
                var meta = recovered.getItemMeta();
                meta.getPersistentDataContainer().set(filterItemsKey, PersistentDataType.STRING, savedFilter);
                recovered.setItemMeta(meta);
            }
        }
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
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null
                || event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                || event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Block clicked = event.getClickedBlock();
        String id = tileItemId(clicked);
        if ("industrial_conduit".equals(id)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§bConduíte industrial: §7conecte caixas do mesmo proprietário pelas faces.");
        } else if ("network_crate".equals(id)) {
            Player player = event.getPlayer();
            String owner = tileOwner(clicked);
            if (owner != null && !player.hasPermission("mechanium.admin")
                    && !owner.equals(player.getUniqueId().toString())) {
                event.setCancelled(true);
                player.sendMessage("§cEsta caixa pertence a outro jogador.");
                return;
            }
            if (!player.isSneaking()) return;

            event.setCancelled(true);
            ItemStack held = player.getInventory().getItemInMainHand();
            String filterId = itemId(held);
            BlockState state = clicked.getState();
            if (!(state instanceof TileState tile)) return;

            Set<String> accepted = filterIds(clicked);
            if (held == null || held.getType().isAir()) {
                tile.getPersistentDataContainer().remove(filterItemsKey);
                if (!tile.update(true, false)) {
                    player.sendMessage("§cNão foi possível limpar o filtro da caixa.");
                    return;
                }
                player.sendMessage("§aFiltro removido. A caixa aceitará todos os itens MECHANIUM.");
                return;
            }
            if (filterId == null || api.getItem(filterId).isEmpty()) {
                player.sendMessage("§eSegure um item MECHANIUM desbloqueado para adicioná-lo/removê-lo do filtro.");
                return;
            }

            Set<String> updated = IndustrialItemFilterPolicy.toggle(accepted, filterId);
            if (updated.isEmpty()) tile.getPersistentDataContainer().remove(filterItemsKey);
            else tile.getPersistentDataContainer().set(filterItemsKey, PersistentDataType.STRING,
                    IndustrialItemFilterPolicy.encode(updated));
            if (!tile.update(true, false)) {
                player.sendMessage("§cNão foi possível salvar o filtro da caixa.");
                return;
            }
            player.sendMessage(updated.contains(filterId)
                    ? "§aFiltro: esta caixa agora aceita " + filterId + "."
                    : "§eFiltro: " + filterId + " removido da lista de aceitação.");
            if (updated.isEmpty()) {
                player.sendMessage("§7A lista ficou vazia; a caixa voltou a aceitar todos os itens MECHANIUM.");
            } else {
                player.sendMessage("§7Agache com a mão vazia para limpar todos os filtros.");
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
        for (Location location : new ArrayList<>(loadedPipes.values())) {
            World world = location.getWorld();
            if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                loadedPipes.remove(key(location));
                continue;
            }
            BlockState state = location.getBlock().getState();
            if (!(state instanceof TileState tile)
                    || !"industrial_conduit".equals(tile.getPersistentDataContainer()
                    .get(itemIdKey, PersistentDataType.STRING))) {
                loadedPipes.remove(key(location));
                continue;
            }
            String owner = tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (owner == null || processed.contains(key(state.getBlock()))) continue;
            Network network = collectNetwork(state.getBlock(), owner);
            if (network.pipes().isEmpty()) continue;
            boolean overlap = network.pipes().stream().anyMatch(processed::contains);
            processed.addAll(network.pipes());
            if (!overlap && network.complete()) moveItemsBatch(network);
        }
    }

    private void registerChunkPipes(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof TileState tile)) continue;
            if ("industrial_conduit".equals(tile.getPersistentDataContainer()
                    .get(itemIdKey, PersistentDataType.STRING))) {
                loadedPipes.put(key(state.getLocation()), state.getLocation());
            }
        }
    }

    @EventHandler
    public void onNetworkChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        registerChunkPipes(event.getChunk());
    }

    @EventHandler
    public void onNetworkChunkUnload(org.bukkit.event.world.ChunkUnloadEvent event) {
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

    private void moveItemsBatch(Network network) {
        List<Inventory> crates = network.crates();
        if (crates.size() < 2) return;
        int configuredLimit = Math.max(1, Math.min(64,
                plugin.getConfig().getInt("industrial-networks.items-per-cycle", 16)));
        int totalSlots = crates.stream().mapToInt(Inventory::getSize).sum();
        if (totalSlots == 0) return;

        String networkId = network.pipes().stream().min(String::compareTo).orElseThrow();
        IndustrialNetworkCursor cursor = transferCursors.computeIfAbsent(
                networkId, ignored -> new IndustrialNetworkCursor());
        int start = cursor.start(totalSlots);

        // Inspect every source slot at most once, beginning after the previous
        // successful transfer. A blocked first slot therefore cannot starve later crates.
        for (int offset = 0; offset < totalSlots; offset++) {
            int flatSlot = (start + offset) % totalSlots;
            int sourceIndex = 0;
            int slot = flatSlot;
            while (sourceIndex < crates.size() && slot >= crates.get(sourceIndex).getSize()) {
                slot -= crates.get(sourceIndex).getSize();
                sourceIndex++;
            }
            if (sourceIndex >= crates.size()) continue;

            Inventory source = crates.get(sourceIndex);
            ItemStack stack = source.getItem(slot);
            if (!isMechaniumItem(stack)) continue;

            int targetStartOffset = cursor.targetOffset(crates.size());
            for (int attempt = 0; attempt < crates.size() - 1; attempt++) {
                int targetOffset = 1 + ((targetStartOffset - 1 + attempt) % (crates.size() - 1));
                int targetIndex = (sourceIndex + targetOffset) % crates.size();
                Inventory target = crates.get(targetIndex);
                if (!acceptsItem(target, stack)) continue;

                int requestedAmount = IndustrialTransferPolicy.batchSize(
                        stack.getAmount(), stack.getMaxStackSize(), configuredLimit);
                if (requestedAmount <= 0) continue;

                ItemStack moved = stack.clone();
                moved.setAmount(requestedAmount);
                Map<Integer, ItemStack> leftovers = target.addItem(moved);
                int leftoverAmount = leftovers.values().stream()
                        .mapToInt(ItemStack::getAmount).sum();
                IndustrialTransferPolicy.TransferResult transfer =
                        IndustrialTransferPolicy.afterInsertion(
                                stack.getAmount(), requestedAmount, leftoverAmount);
                if (transfer.insertedAmount() <= 0) continue;

                if (transfer.sourceRemaining() == 0) source.setItem(slot, null);
                else {
                    ItemStack remaining = stack.clone();
                    remaining.setAmount(transfer.sourceRemaining());
                    source.setItem(slot, remaining);
                }
                cursor.advance(totalSlots, flatSlot, crates.size(), targetOffset);
                return;
            }
        }
        // Rotate even on a blocked cycle, so a temporarily full destination
        // does not pin the scheduler to the same source slot forever.
        cursor.rotateAfterBlockedCycle(totalSlots, crates.size());
    }

    private boolean acceptsItem(Inventory target, ItemStack item) {
        if (target == null || item == null) return false;
        Location location = target.getLocation();
        if (location == null || location.getWorld() == null
                || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return false;
        }
        return IndustrialItemFilterPolicy.accepts(filterIds(location.getBlock()), itemId(item));
    }

    private Set<String> filterIds(Block crate) {
        if (crate == null || !(crate.getState() instanceof TileState tile)) return Set.of();
        String encoded = tile.getPersistentDataContainer().get(filterItemsKey, PersistentDataType.STRING);
        return IndustrialItemFilterPolicy.decode(encoded);
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

    private String key(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":"
                + location.getBlockY() + ":" + location.getBlockZ();
    }

    private record Network(Set<String> pipes, List<Inventory> crates, boolean complete) { }
}
