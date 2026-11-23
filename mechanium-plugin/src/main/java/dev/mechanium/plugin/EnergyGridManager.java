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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Renewable generators charge adjacent batteries; powered machines draw from connected batteries. */
final class EnergyGridManager implements Listener {
    private static final BlockFace[] FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN
    };
    private static final Set<String> ENERGY_ITEMS = Set.of("wind_rotor", "solar_cell", "energy_cell", "atomic_reactor");
    private static final Set<String> POWERED_MACHINES = Set.of(
            "water_filter_machine", "advanced_assembly_machine",
            "industrial_crusher_machine", "industrial_smelter_machine", "industrial_mill_machine", "industrial_canner_machine",
            "chicken_incubator_machine", "cow_incubator_machine", "pig_incubator_machine", "sheep_incubator_machine",
            "rapid_press_machine", "rapid_coil_winder_machine", "air_purifier_machine");
    private static final int BATTERY_CAPACITY = 10_000;

    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey chargeKey;
    private final NamespacedKey reactorFuelSecondsKey;
    private BukkitTask task;

    EnergyGridManager(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
        this.itemIdKey = NamespacedKey.fromString("mechanium:item_id");
        this.ownerKey = new NamespacedKey(plugin, "energy_owner");
        this.chargeKey = new NamespacedKey(plugin, "energy_charge");
        this.reactorFuelSecondsKey = new NamespacedKey(plugin, "reactor_fuel_seconds");
    }

    void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::generate, 20L, 20L);
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    boolean requiresPower(String machineId) {
        return POWERED_MACHINES.contains(machineId);
    }

    boolean consume(UUID owner, Location machine, int amount) {
        if (owner == null || machine == null || machine.getWorld() == null || amount < 1) return false;
        String ownerId = owner.toString();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        for (BlockFace face : FACES) {
            Block adjacent = adjacentLoaded(machine.getBlock(), face);
            if (adjacent != null && isBattery(adjacent, ownerId)) queue.add(adjacent);
        }
        while (!queue.isEmpty() && visited.size() < 64) {
            Block block = queue.removeFirst();
            if (!visited.add(key(block))) continue;
            BlockState state = block.getState();
            if (!(state instanceof TileState tile)) continue;
            int charge = tile.getPersistentDataContainer().getOrDefault(
                    chargeKey, PersistentDataType.INTEGER, 0);
            if (charge >= amount) {
                tile.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, charge - amount);
                tile.update(true, false);
                return true;
            }
            for (BlockFace face : FACES) {
                Block adjacent = adjacentLoaded(block, face);
                if (adjacent != null && isBattery(adjacent, ownerId) && !visited.contains(key(adjacent))) {
                    queue.addLast(adjacent);
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        String id = itemId(event.getItemInHand());
        if (id == null || !ENERGY_ITEMS.contains(id)) return;
        Material expected = switch (id) {
            case "wind_rotor" -> Material.FURNACE;
            case "solar_cell" -> Material.SMOKER;
            case "energy_cell" -> Material.BARREL;
            case "atomic_reactor" -> Material.BLAST_FURNACE;
            case "atomic_reactor" -> Material.BLAST_FURNACE;
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
            tile.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, 0);
        }
        if (id.equals("atomic_reactor")) {
            tile.getPersistentDataContainer().set(reactorFuelSecondsKey, PersistentDataType.INTEGER, 0);
        }
        if (!tile.update(true, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNão foi possível registrar o componente energético.");
        }
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
        } else if ("wind_rotor".equals(id) || "solar_cell".equals(id)) {
            event.getPlayer().sendMessage("§eGerador MECHANIUM: §f" + (id.equals("wind_rotor") ? "eólico" : "solar")
                    + " §7— conecte uma bateria por uma face.");
        } else if ("atomic_reactor".equals(id) && state instanceof org.bukkit.block.TileState tile) {
            int seconds = tile.getPersistentDataContainer().getOrDefault(
                    reactorFuelSecondsKey, PersistentDataType.INTEGER, 0);
            event.getPlayer().sendMessage("§eGerador atômico civil: §f" + seconds
                    + "s de combustível restante §7— coloque uma Célula energética selada no espaço de combustível.");
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
        ItemStack remainingFuel = null;
        if ("atomic_reactor".equals(id) && state instanceof org.bukkit.block.Container container) {
            ItemStack fuel = container.getInventory().getItem(1);
            if (isReactorFuel(fuel)) remainingFuel = fuel.clone();
        }
        final ItemStack fuelToDrop = remainingFuel;
        Location drop = event.getBlock().getLocation().add(0.5, 0.5, 0.5);
        Block block = event.getBlock();
        event.setDropItems(false);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!event.isCancelled() && block.getType().isAir()) {
                block.getWorld().dropItemNaturally(drop, recovered);
                if (fuelToDrop != null) block.getWorld().dropItemNaturally(drop, fuelToDrop);
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
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof TileState tile)) continue;
                    String id = tile.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
                    String owner = tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
                    if (owner == null) continue;
                    if ("atomic_reactor".equals(id)) {
                        generateReactor(state, tile, owner);
                        continue;
                    }
                    if (!"wind_rotor".equals(id) && !"solar_cell".equals(id)) continue;
                    int production = "wind_rotor".equals(id) ? 1
                            : (world.getTime() < 12_000 && !world.hasStorm() ? 2 : 0);
                    if (production > 0) chargeAdjacentBattery(state.getBlock(), owner, production);
                }
            }
        }
    }

    private void generateReactor(BlockState state, TileState tile, String owner) {
        if (!(state instanceof org.bukkit.block.Container container)) return;
        int remaining = tile.getPersistentDataContainer().getOrDefault(
                reactorFuelSecondsKey, PersistentDataType.INTEGER, 0);
        if (remaining <= 0) {
            ItemStack fuel = container.getInventory().getItem(1);
            if (!isReactorFuel(fuel)) return;
            if (fuel.getAmount() <= 1) container.getInventory().setItem(1, null);
            else {
                fuel.setAmount(fuel.getAmount() - 1);
                container.getInventory().setItem(1, fuel);
            }
            remaining = Math.max(1, plugin.getConfig().getInt("energy.atomic-reactor.fuel-seconds", 600));
        }
        int output = Math.max(1, plugin.getConfig().getInt("energy.atomic-reactor.output-per-second", 12));
        if (chargeAdjacentBattery(state.getBlock(), owner, output)) {
            tile.getPersistentDataContainer().set(reactorFuelSecondsKey, PersistentDataType.INTEGER,
                    Math.max(0, remaining - 1));
            tile.update(true, false);
        }
    }

    private boolean isReactorFuel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return "reactor_fuel_cell".equals(item.getItemMeta().getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING));
    }

    private boolean chargeAdjacentBattery(Block generator, String owner, int production) {
        for (BlockFace face : FACES) {
            Block adjacent = adjacentLoaded(generator, face);
            if (adjacent == null || !isBattery(adjacent, owner)) continue;
            BlockState state = adjacent.getState();
            if (!(state instanceof TileState tile)) continue;
            int charge = tile.getPersistentDataContainer().getOrDefault(chargeKey, PersistentDataType.INTEGER, 0);
            if (charge >= BATTERY_CAPACITY) continue;
            int updated = Math.min(BATTERY_CAPACITY, charge + production);
            tile.getPersistentDataContainer().set(chargeKey, PersistentDataType.INTEGER, updated);
            tile.update(true, false);
            return true;
        }
        return false;
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

    private String key(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
