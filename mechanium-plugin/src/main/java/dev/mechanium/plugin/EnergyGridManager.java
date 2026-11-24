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
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Renewable generators charge adjacent batteries; powered machines draw from connected batteries. */
final class EnergyGridManager implements Listener {
    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };
    private static final Set<String> ENERGY_ITEMS = Set.of(
            "wind_rotor", "solar_cell", "energy_cell", "abstract_reactor", "energy_conduit");
    private static final int BATTERY_CAPACITY = 10_000;
    private static final int MAX_NETWORK_NODES = 256;

    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey chargeKey;
    private final NamespacedKey reactorFuelUntilKey;
    private final Map<String, Location> loadedGenerators = new HashMap<>();
    private BukkitTask task;

    EnergyGridManager(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
        this.itemIdKey = NamespacedKey.fromString("mechanium:item_id");
        this.ownerKey = new NamespacedKey(plugin, "energy_owner");
        this.chargeKey = new NamespacedKey(plugin, "energy_charge");
        this.reactorFuelUntilKey = new NamespacedKey(plugin, "reactor_fuel_until");
    }

    void start() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) registerChunkGenerators(chunk);
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::generate, 20L, 20L);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        loadedGenerators.clear();
    }

    boolean consume(UUID owner, Location machine, int amount) {
        if (amount <= 0) return true;
        if (owner == null || machine == null || machine.getWorld() == null) return false;

        EnergyNetwork network = collectConnectedEnergyNetwork(machine.getBlock(), owner.toString());
        if (!network.complete() || network.batteries().isEmpty()) return false;

        List<Integer> currentCharges = network.batteries().stream().map(this::batteryCharge).toList();
        List<Integer> newCharges = EnergyAllocationPolicy.planConsumption(currentCharges, amount).orElse(null);
        if (newCharges == null) return false;

        List<ChargeChange> applied = new ArrayList<>();
        for (int i = 0; i < network.batteries().size(); i++) {
            int oldCharge = currentCharges.get(i);
            int newCharge = newCharges.get(i);
            if (oldCharge == newCharge) continue;

            Block battery = network.batteries().get(i);
            BlockState state = battery.getState();
            if (!(state instanceof TileState tile)) {
                rollbackChargeChanges(applied);
                return false;
            }
            tile.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, newCharge);
            if (!tile.update(true, false)) {
                rollbackChargeChanges(applied);
                return false;
            }
            applied.add(new ChargeChange(battery, oldCharge, newCharge));
        }
        return true;
    }

    private void rollbackChargeChanges(List<ChargeChange> applied) {
        for (int i = applied.size() - 1; i >= 0; i--) {
            ChargeChange change = applied.get(i);
            BlockState state = change.block().getState();
            if (state instanceof TileState tile) {
                tile.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, change.oldCharge());
                tile.update(true, false);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        String id = itemId(event.getItemInHand());
        if (id == null || !ENERGY_ITEMS.contains(id)) return;
        Material expected = switch (id) {
            case "wind_rotor" -> Material.FURNACE;
            case "solar_cell" -> Material.SMOKER;
            case "energy_cell" -> Material.BARREL;
            case "abstract_reactor" -> Material.BLAST_FURNACE;
            case "energy_conduit" -> Material.HOPPER;
            default -> Material.AIR;
        };
        Block block = event.getBlockPlaced();
        if (block.getType() != expected || !(block.getState() instanceof TileState tile)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cEste componente precisa ser colocado como " + expected.name() + ".");
            return;
        }
        tile.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, id);
        tile.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                event.getPlayer().getUniqueId().toString());
        if (id.equals("energy_cell")) {
            ItemStack placedItem = event.getItemInHand();
            int storedCharge = 0;
            if (placedItem.hasItemMeta()) {
                storedCharge = placedItem.getItemMeta().getPersistentDataContainer()
                        .getOrDefault(chargeKey, PersistentDataType.INTEGER, 0);
            }
            tile.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER,
                    Math.max(0, Math.min(BATTERY_CAPACITY, storedCharge)));
        }
        if (!tile.update(true, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNão foi possível registrar o componente energético.");
            return;
        }
        if (isGenerator(id)) loadedGenerators.put(key(block.getLocation()), block.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null || (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
                && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR)) return;
        String id = tileItemId(event.getClickedBlock());
        if (id == null || !ENERGY_ITEMS.contains(id)) return;
        event.setCancelled(true);
        BlockState state = event.getClickedBlock().getState();
        if (state instanceof TileState tile && "energy_cell".equals(id)) {
            int charge = tile.getPersistentDataContainer().getOrDefault(chargeKey, PersistentDataType.INTEGER, 0);
            event.getPlayer().sendMessage("§eBateria MECHANIUM: §f" + charge + "§7 / " + BATTERY_CAPACITY + " unidades");
        } else if (state instanceof TileState reactorTile && "abstract_reactor".equals(id)) {
            ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
            if ("reactor_crystal".equals(itemId(hand))) {
                String owner = reactorTile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
                if (owner != null && !owner.equals(event.getPlayer().getUniqueId().toString())
                        && !event.getPlayer().hasPermission("mechanium.admin")) {
                    event.getPlayer().sendMessage("§cEste gerador pertence a outro jogador.");
                    return;
                }
                if (hand.getAmount() <= 1) event.getPlayer().getInventory().setItemInMainHand(null);
                else hand.setAmount(hand.getAmount() - 1);
                reactorTile.getPersistentDataContainer().set(reactorFuelUntilKey, PersistentDataType.LONG,
                        System.currentTimeMillis() + 300_000L);
                reactorTile.update(true, false);
                event.getPlayer().sendMessage("§dCristal energético inserido. O gerador produzirá energia por cinco minutos.");
            } else {
                long until = reactorTile.getPersistentDataContainer().getOrDefault(reactorFuelUntilKey,
                        PersistentDataType.LONG, 0L);
                long remaining = Math.max(0L, (until - System.currentTimeMillis()) / 1000L);
                event.getPlayer().sendMessage("§dGerador cristalino: §f" + remaining
                        + "s de combustível restante. §7Use um cristal energético para recarregar.");
            }
        } else if ("energy_conduit".equals(id)) {
            String owner = tileOwner(event.getClickedBlock());
            event.getPlayer().sendMessage("§bConduíte energético: §7conecta geradores, baterias e máquinas do mesmo proprietário.");
            if (owner != null && !owner.equals(event.getPlayer().getUniqueId().toString())
                    && !event.getPlayer().hasPermission("mechanium.admin")) {
                event.getPlayer().sendMessage("§7Rede pertencente a outro jogador; não é possível retirar energia dela.");
            }
        } else if ("wind_rotor".equals(id) || "solar_cell".equals(id)) {
            event.getPlayer().sendMessage("§eGerador MECHANIUM: §f" + (id.equals("wind_rotor") ? "eólico" : "solar")
                    + " §7— conecte a bateria diretamente ou use conduítes energéticos.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        String id = tileItemId(event.getBlock());
        if (id == null || !ENERGY_ITEMS.contains(id)) return;
        BlockState state = event.getBlock().getState();
        if (!(state instanceof TileState tile)) return;
        String owner = tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (owner != null && !event.getPlayer().hasPermission("mechanium.admin")
                && !owner.equals(event.getPlayer().getUniqueId().toString())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cSomente o proprietário ou um administrador pode desmontar este componente.");
            return;
        }
        ItemStack recovered = api.createItemStack(id, 1).orElseGet(() -> new ItemStack(event.getBlock().getType()));
        if ("energy_cell".equals(id) && recovered.hasItemMeta()) {
            int charge = tile.getPersistentDataContainer().getOrDefault(chargeKey, PersistentDataType.INTEGER, 0);
            var itemMeta = recovered.getItemMeta();
            itemMeta.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER,
                    Math.max(0, Math.min(BATTERY_CAPACITY, charge)));
            recovered.setItemMeta(itemMeta);
        }
        Location drop = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        Block block = event.getBlock();
        event.setDropItems(false);
        String locationKey = key(block.getLocation());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!event.isCancelled() && block.getType().isAir()) {
                loadedGenerators.remove(locationKey);
                block.getWorld().dropItemNaturally(drop, recovered);
            } else if (isGenerator(id) && !event.isCancelled()) {
                loadedGenerators.put(locationKey, block.getLocation());
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isEnergyNodeAt(event.getSource().getLocation())
                || isEnergyNodeAt(event.getDestination().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isEnergyNode);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isEnergyNode);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isEnergyNode)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isEnergyNode)) event.setCancelled(true);
    }

    private void generate() {
        for (Location location : new ArrayList<>(loadedGenerators.values())) {
            World world = location.getWorld();
            if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                loadedGenerators.remove(key(location));
                continue;
            }
            BlockState state = location.getBlock().getState();
            if (!(state instanceof TileState tile)) {
                loadedGenerators.remove(key(location));
                continue;
            }
            String id = tile.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
            if (!isGenerator(id)) {
                loadedGenerators.remove(key(location));
                continue;
            }
            String owner = tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
            if (owner == null) continue;

            int production;
            if ("wind_rotor".equals(id)) {
                production = 1;
            } else if ("solar_cell".equals(id)) {
                production = world.getTime() < 12_000 && !world.hasStorm() ? 2 : 0;
            } else {
                long fuelUntil = tile.getPersistentDataContainer().getOrDefault(
                        reactorFuelUntilKey, PersistentDataType.LONG, 0L);
                production = fuelUntil > System.currentTimeMillis() ? 8 : 0;
            }
            if (production > 0) chargeConnectedBatteries(state.getBlock(), owner, production);
        }
    }

    private void registerChunkGenerators(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof TileState tile)) continue;
            String id = tile.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
            if (isGenerator(id)) loadedGenerators.put(key(state.getLocation()), state.getLocation());
        }
    }

    private boolean isGenerator(String id) {
        return "wind_rotor".equals(id) || "solar_cell".equals(id) || "abstract_reactor".equals(id);
    }

    @EventHandler
    public void onEnergyChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        registerChunkGenerators(event.getChunk());
    }

    @EventHandler
    public void onEnergyChunkUnload(org.bukkit.event.world.ChunkUnloadEvent event) {
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        UUID worldId = event.getWorld().getUID();
        loadedGenerators.entrySet().removeIf(entry -> {
            Location location = entry.getValue();
            return location.getWorld() != null && location.getWorld().getUID().equals(worldId)
                    && (location.getBlockX() >> 4) == chunkX && (location.getBlockZ() >> 4) == chunkZ;
        });
    }

    private void chargeConnectedBatteries(Block generator, String owner, int production) {
        EnergyNetwork network = collectConnectedEnergyNetwork(generator, owner);
        if (!network.complete() || network.batteries().isEmpty()) return;

        List<Block> batteries = new ArrayList<>(network.batteries());
        batteries.sort(Comparator.comparingInt(this::batteryCharge));
        List<Integer> currentCharges = batteries.stream().map(this::batteryCharge).toList();
        List<Integer> newCharges = EnergyAllocationPolicy.planGeneration(
                currentCharges, production, BATTERY_CAPACITY);

        List<ChargeChange> applied = new ArrayList<>();
        for (int i = 0; i < batteries.size(); i++) {
            int oldCharge = currentCharges.get(i);
            int newCharge = newCharges.get(i);
            if (oldCharge == newCharge) continue;

            Block battery = batteries.get(i);
            BlockState state = battery.getState();
            if (!(state instanceof TileState tile)) {
                rollbackChargeChanges(applied);
                return;
            }
            tile.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, newCharge);
            if (!tile.update(true, false)) {
                rollbackChargeChanges(applied);
                return;
            }
            applied.add(new ChargeChange(battery, oldCharge, newCharge));
        }
    }

    private EnergyNetwork collectConnectedEnergyNetwork(Block anchor, String owner) {
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        List<Block> batteries = new ArrayList<>();
        for (BlockFace face : FACES) {
            Block adjacent = adjacentLoaded(anchor, face);
            if (adjacent != null && isEnergyLink(adjacent, owner)) queue.addLast(adjacent);
        }

        while (!queue.isEmpty() && visited.size() < MAX_NETWORK_NODES) {
            Block block = queue.removeFirst();
            if (!visited.add(key(block)) || !isEnergyLink(block, owner)) continue;
            if (isBattery(block, owner)) batteries.add(block);
            for (BlockFace face : FACES) {
                Block adjacent = adjacentLoaded(block, face);
                if (adjacent != null && isEnergyLink(adjacent, owner) && !visited.contains(key(adjacent))) {
                    queue.addLast(adjacent);
                }
            }
        }
        return new EnergyNetwork(List.copyOf(batteries), queue.isEmpty());
    }

    private boolean isEnergyLink(Block block, String owner) {
        return isBattery(block, owner) || isConduit(block, owner);
    }

    private boolean isConduit(Block block, String owner) {
        if (block == null || block.getType() != Material.HOPPER) return false;
        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) return false;
        String id = tile.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        String blockOwner = tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return "energy_conduit".equals(id) && owner.equals(blockOwner);
    }

    private int batteryCharge(Block battery) {
        BlockState state = battery.getState();
        if (!(state instanceof TileState tile)) return 0;
        return Math.max(0, Math.min(BATTERY_CAPACITY,
                tile.getPersistentDataContainer().getOrDefault(chargeKey, PersistentDataType.INTEGER, 0)));
    }

    private boolean isBattery(Block block, String owner) {
        if (block == null || block.getType() != Material.BARREL) return false;
        BlockState state = block.getState();
        if (!(state instanceof TileState tile)) return false;
        String id = tile.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
        String blockOwner = tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return "energy_cell".equals(id) && owner.equals(blockOwner);
    }

    private boolean isEnergyNodeAt(Location location) {
        return location != null && location.getWorld() != null
                && location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
                && isEnergyNode(location.getBlock());
    }

    private boolean isEnergyNode(Block block) {
        String id = tileItemId(block);
        return id != null && ENERGY_ITEMS.contains(id);
    }

    private String tileItemId(Block block) {
        if (block == null || !(block.getState() instanceof TileState tile)) return null;
        return tile.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
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

    private String tileOwner(Block block) {
        if (block == null || !(block.getState() instanceof TileState tile)) return null;
        return tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
    }

    private String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private String key(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":"
                + location.getBlockY() + ":" + location.getBlockZ();
    }

    private record EnergyNetwork(List<Block> batteries, boolean complete) { }
    private record ChargeChange(Block block, int oldCharge, int newCharge) { }
}
