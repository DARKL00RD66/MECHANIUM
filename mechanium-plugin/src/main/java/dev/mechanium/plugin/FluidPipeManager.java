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
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Typed-fluid tanks connected by owner-matched conduits.
 * Legacy MECHANIUM fluid items stored in tank inventories are converted to millibuckets.
 */
final class FluidPipeManager implements Listener {
    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };
    private static final int MAX_PIPE_NETWORK = 128;
    private static final int MILLIBUCKETS_PER_ITEM = 250;
    private static final int DEFAULT_TANK_CAPACITY_MB = 16_000;
    private static final int DEFAULT_TRANSFER_RATE_MB = 250;

    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey fluidIdKey;
    private final NamespacedKey fluidAmountKey;
    private final Map<String, Location> loadedPipes = new HashMap<>();
    private BukkitTask task;

    FluidPipeManager(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
        this.itemIdKey = NamespacedKey.fromString("mechanium:item_id");
        this.ownerKey = new NamespacedKey(plugin, "fluid_network_owner");
        this.fluidIdKey = new NamespacedKey(plugin, "tank_fluid_id");
        this.fluidAmountKey = new NamespacedKey(plugin, "tank_fluid_amount_mb");
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
        if ("liquid_tank".equals(id)) {
            tile.getPersistentDataContainer().set(fluidAmountKey, PersistentDataType.INTEGER, 0);
            tile.getPersistentDataContainer().remove(fluidIdKey);
        }
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
        FluidTankContents storedFluid = FluidTankContents.empty();
        if ("liquid_tank".equals(id)) {
            migrateLegacyTankInventory(block);
            BlockState currentState = block.getState();
            if (currentState instanceof Container container) {
                for (ItemStack item : container.getInventory().getContents()) {
                    if (item != null && !item.getType().isAir()) contents.add(item.clone());
                }
            }
            storedFluid = readTankContents(block);
        }

        ItemStack recovered = api.createItemStack(id, 1).orElseGet(() -> new ItemStack(block.getType()));
        List<ItemStack> fluidDrops = fluidItemsFor(storedFluid);
        Location drop = block.getLocation().add(0.5, 0.5, 0.5);
        event.setDropItems(false);
        String locationKey = key(block.getLocation());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || !block.getType().isAir()) return;
            loadedPipes.remove(locationKey);
            for (ItemStack item : contents) block.getWorld().dropItemNaturally(drop, item);
            for (ItemStack item : fluidDrops) block.getWorld().dropItemNaturally(drop, item);
            block.getWorld().dropItemNaturally(drop, recovered);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPipeInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND) return;
        Block clicked = event.getClickedBlock();
        String id = tileItemId(clicked);
        if ("fluid_pipe".equals(id)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§bTubulação MECHANIUM: §7conecta tanques pelas faces e transporta fluidos compatíveis.");
            return;
        }
        if (!"liquid_tank".equals(id)) return;

        Player player = event.getPlayer();
        String owner = tileOwner(clicked);
        if (owner != null && !player.hasPermission("mechanium.admin")
                && !owner.equals(player.getUniqueId().toString())) {
            event.setCancelled(true);
            player.sendMessage("§cEste tanque pertence a outro jogador.");
            return;
        }

        migrateLegacyTankInventory(clicked);
        FluidTankContents current = readTankContents(clicked);
        ItemStack hand = player.getInventory().getItemInMainHand();
        FluidItem fluidItem = fluidItem(hand);

        if (fluidItem != null) {
            event.setCancelled(true);
            if (!api.canUseItem(player.getUniqueId(), fluidItem.itemId())
                    && !player.hasPermission("mechanium.admin")) {
                player.sendMessage("§cVocê ainda não desbloqueou o uso deste fluido.");
                return;
            }
            FluidTankPolicy.FillResult filled = FluidTankPolicy.fill(
                    current, fluidItem.fluidId(), fluidItem.millibucketsPerItem(), tankCapacityMb());
            if (filled.acceptedMb() != fluidItem.millibucketsPerItem()) {
                player.sendMessage("§c" + (current.isEmpty() ? "O tanque está cheio." : "O tanque já contém outro fluido ou não tem espaço suficiente."));
                return;
            }
            if (!writeTankContents(clicked, filled.contents())) {
                player.sendMessage("§cNão foi possível salvar o volume do tanque.");
                return;
            }
            consumeOneFromHand(player, hand);
            player.sendMessage("§aTanque abastecido: " + filled.contents().fluidId()
                    + " — " + filled.contents().amountMb() + "/" + tankCapacityMb() + " mB.");
            return;
        }

        if (player.isSneaking() && hand.getType().isAir()) {
            event.setCancelled(true);
            extractOneFluidUnit(player, clicked, current);
            return;
        }

        player.sendMessage("§7" + (current.isEmpty()
                ? "Tanque vazio. Coloque itens de fluido MECHANIUM no inventário do tanque ou use-os diretamente no bloco."
                : "Tanque: " + current.fluidId() + " — " + current.amountMb() + "/" + tankCapacityMb() + " mB. Agache com a mão vazia para retirar 250 mB."));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isTransportNodeAt(event.getSource().getLocation())
                || isTransportNodeAt(event.getDestination().getLocation())) {
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
            if (!overlapsProcessedNetwork && network.complete()) moveFluid(network.tanks());
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
        List<Block> tanks = new ArrayList<>();
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
                    if (!tanks.contains(adjacent)) tanks.add(adjacent);
                }
            }
        }
        return new Network(visited, tanks, queue.isEmpty());
    }

    private void moveFluid(List<Block> tanks) {
        if (tanks.size() < 2) return;
        for (Block tank : tanks) migrateLegacyTankInventory(tank);

        for (int sourceIndex = 0; sourceIndex < tanks.size(); sourceIndex++) {
            Block source = tanks.get(sourceIndex);
            FluidTankContents sourceContents = readTankContents(source);
            if (sourceContents.isEmpty()) continue;
            for (int targetIndex = 0; targetIndex < tanks.size(); targetIndex++) {
                if (targetIndex == sourceIndex) continue;
                Block target = tanks.get(targetIndex);
                FluidTankPolicy.TransferResult plan = FluidTankPolicy.transfer(
                        sourceContents, readTankContents(target), transferRateMb(), tankCapacityMb());
                if (plan.transferredMb() <= 0) continue;
                if (persistTransfer(source, target, plan)) return;
            }
        }
    }

    private boolean persistTransfer(Block source, Block target, FluidTankPolicy.TransferResult plan) {
        FluidTankContents originalSource = readTankContents(source);
        FluidTankContents originalTarget = readTankContents(target);
        if (!writeTankContents(target, plan.target())) return false;
        if (!writeTankContents(source, plan.source())) {
            writeTankContents(target, originalTarget);
            writeTankContents(source, originalSource);
            return false;
        }
        return true;
    }

    private void migrateLegacyTankInventory(Block block) {
        if (block == null) return;
        BlockState blockState = block.getState();
        if (!(blockState instanceof Container container) || !(blockState instanceof TileState tile)) return;
        Inventory inventory = container.getInventory();
        String ownerText = tileOwner(block);
        if (ownerText == null) return;
        UUID owner;
        try {
            owner = UUID.fromString(ownerText);
        } catch (IllegalArgumentException exception) {
            return;
        }
        FluidTankContents current = readTankContents(block);
        boolean changed = false;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            FluidItem offer = fluidItem(stack);
            if (offer == null || !api.canUseItem(owner, offer.itemId())) continue;
            int room = Math.max(0, tankCapacityMb() - current.amountMb());
            int maxItems = Math.min(stack.getAmount(), room / offer.millibucketsPerItem());
            if (maxItems <= 0) continue;
            FluidTankPolicy.FillResult fill = FluidTankPolicy.fill(current, offer.fluidId(),
                    maxItems * offer.millibucketsPerItem(), tankCapacityMb());
            int consumed = fill.acceptedMb() / offer.millibucketsPerItem();
            if (consumed <= 0) continue;

            current = fill.contents();
            int remaining = stack.getAmount() - consumed;
            if (remaining == 0) inventory.setItem(slot, null);
            else {
                ItemStack left = stack.clone();
                left.setAmount(remaining);
                inventory.setItem(slot, left);
            }
            changed = true;
        }

        if (changed) {
            writeContents(tile, current);
            container.update(true, false);
        }
    }

    private FluidTankContents readTankContents(Block block) {
        if (block == null || !(block.getState() instanceof TileState tile)) return FluidTankContents.empty();
        String id = tile.getPersistentDataContainer().get(fluidIdKey, PersistentDataType.STRING);
        int amount = Math.max(0, Math.min(tankCapacityMb(),
                tile.getPersistentDataContainer().getOrDefault(fluidAmountKey, PersistentDataType.INTEGER, 0)));
        if (amount == 0 || id == null) return FluidTankContents.empty();
        try {
            return new FluidTankContents(id, amount);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("ID de fluido inválido em tanque MECHANIUM em "
                    + block.getWorld().getName() + " " + block.getX() + "," + block.getY() + "," + block.getZ());
            return FluidTankContents.empty();
        }
    }

    private boolean writeTankContents(Block block, FluidTankContents contents) {
        if (block == null || !(block.getState() instanceof TileState tile)) return false;
        writeContents(tile, contents);
        return tile.update(true, false);
    }

    private void writeContents(TileState tile, FluidTankContents contents) {
        if (contents.isEmpty()) {
            tile.getPersistentDataContainer().remove(fluidIdKey);
            tile.getPersistentDataContainer().set(fluidAmountKey, PersistentDataType.INTEGER, 0);
        } else {
            tile.getPersistentDataContainer().set(fluidIdKey, PersistentDataType.STRING, contents.fluidId());
            tile.getPersistentDataContainer().set(fluidAmountKey, PersistentDataType.INTEGER, contents.amountMb());
        }
    }

    private void extractOneFluidUnit(Player player, Block tank, FluidTankContents current) {
        if (current.isEmpty()) {
            player.sendMessage("§7O tanque está vazio.");
            return;
        }
        if (current.amountMb() < MILLIBUCKETS_PER_ITEM) {
            player.sendMessage("§cO tanque não contém volume suficiente para retirar um item (250 mB).");
            return;
        }
        FluidTankPolicy.DrainResult drained = FluidTankPolicy.drain(current, MILLIBUCKETS_PER_ITEM);
        if (drained.removedMb() != MILLIBUCKETS_PER_ITEM) {
            player.sendMessage("§cNão foi possível calcular a retirada do fluido.");
            return;
        }
        String outputItemId = outputItemFor(current.fluidId());
        if (outputItemId == null) {
            player.sendMessage("§cNão existe um item de saída configurado para este fluido.");
            return;
        }
        if (!api.canUseItem(player.getUniqueId(), outputItemId)
                && !player.hasPermission("mechanium.admin")) {
            player.sendMessage("§cVocê ainda não desbloqueou o uso deste fluido.");
            return;
        }
        ItemStack output = api.createItemStack(outputItemId, 1).orElse(null);
        if (output == null || !writeTankContents(tank, drained.contents())) {
            player.sendMessage("§cNão foi possível retirar o fluido do tanque.");
            return;
        }
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(output);
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.sendMessage("§aRetirados " + drained.removedMb() + " mB de " + current.fluidId() + ".");
    }

    private List<ItemStack> fluidItemsFor(FluidTankContents contents) {
        List<ItemStack> drops = new ArrayList<>();
        if (contents.isEmpty()) return drops;
        String itemId = outputItemFor(contents.fluidId());
        if (itemId == null) return drops;
        int units = contents.amountMb() / MILLIBUCKETS_PER_ITEM;
        int maxStackSize = api.getItem(itemId).map(item -> item.maxStackSize()).orElse(16);
        while (units > 0) {
            int stackSize = Math.min(maxStackSize, units);
            ItemStack stack = api.createItemStack(itemId, stackSize).orElse(null);
            if (stack == null) break;
            drops.add(stack);
            units -= stackSize;
        }
        return drops;
    }

    private String messageError(String value) { return "§c" + value; }
    private String messageInfo(String value) { return "§7" + value; }
    private String messageSuccess(String value) { return "§a" + value; }

    private FluidItem fluidItem(ItemStack item) {
        String id = itemId(item);
        if (id == null) return null;
        return switch (id) {
            case "purified_water" -> new FluidItem(id, "purified_water", MILLIBUCKETS_PER_ITEM);
            case "purified_water_cell" -> new FluidItem(id, "purified_water", 1_000);
            case "coolant_cell" -> new FluidItem(id, "industrial_coolant", MILLIBUCKETS_PER_ITEM);
            default -> null;
        };
    }

    private String outputItemFor(String fluidId) {
        return switch (fluidId) {
            case "purified_water" -> "purified_water";
            case "industrial_coolant" -> "coolant_cell";
            default -> null;
        };
    }

    private void consumeOneFromHand(Player player, ItemStack hand) {
        if (hand.getAmount() <= 1) player.getInventory().setItemInMainHand(null);
        else hand.setAmount(hand.getAmount() - 1);
    }

    private int tankCapacityMb() {
        int configured = Math.max(250, plugin.getConfig().getInt("fluids.tank-capacity-mb", DEFAULT_TANK_CAPACITY_MB));
        return configured - configured % MILLIBUCKETS_PER_ITEM;
    }

    private int transferRateMb() {
        int configured = Math.max(250, plugin.getConfig().getInt("fluids.transfer-mb-per-cycle", DEFAULT_TRANSFER_RATE_MB));
        return configured - configured % MILLIBUCKETS_PER_ITEM;
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

    private String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private String key(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":"
                + location.getBlockY() + ":" + location.getBlockZ();
    }

    private record FluidItem(String itemId, String fluidId, int millibucketsPerItem) { }
    private record Network(Set<String> pipes, List<Block> tanks, boolean complete) { }
}
