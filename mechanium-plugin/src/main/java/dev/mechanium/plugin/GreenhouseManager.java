package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Local greenhouse controller. It gives one extra growth stage to compatible crops
 * after a normal growth event, consuming energy from the owner's connected network.
 * It never places, breaks, or replaces crop blocks itself.
 */
final class GreenhouseManager implements Listener {
    private static final String CONTROLLER_ID = "greenhouse_controller";
    private static final int DEFAULT_RADIUS = 4;
    private static final int MAX_RADIUS = 8;

    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final EnergyGridManager energyGridManager;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey ownerKey;
    private final Map<String, Location> loadedControllers = new HashMap<>();
    private final Map<String, Map<String, Location>> controllersByChunk = new HashMap<>();

    GreenhouseManager(JavaPlugin plugin, MechaniumApi api, EnergyGridManager energyGridManager) {
        this.plugin = plugin;
        this.api = api;
        this.energyGridManager = energyGridManager;
        this.itemIdKey = NamespacedKey.fromString("mechanium:item_id");
        this.ownerKey = new NamespacedKey(plugin, "greenhouse_controller_owner");
    }

    void start() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) registerChunk(chunk);
        }
    }

    void stop() {
        loadedControllers.clear();
        controllersByChunk.clear();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!CONTROLLER_ID.equals(itemId(event.getItemInHand()))) return;
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.DISPENSER || !(block.getState() instanceof TileState tile)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cO controlador climático precisa ser colocado como dispenser.");
            return;
        }
        if (!api.canUseItem(event.getPlayer().getUniqueId(), CONTROLLER_ID)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cVocê ainda não desbloqueou o controlador climático.");
            return;
        }

        tile.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, CONTROLLER_ID);
        tile.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                event.getPlayer().getUniqueId().toString());
        if (!tile.update(true, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNão foi possível registrar o controlador climático.");
            return;
        }
        registerController(block);
        event.getPlayer().sendMessage("§aControlador instalado. Cultivos compatíveis no raio configurado recebem crescimento adicional quando há energia.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null
                || event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (!isController(block)) return;
        event.setCancelled(true);

        Player player = event.getPlayer();
        String ownerText = ownerId(block);
        if (ownerText == null || !canManage(player, ownerText)) {
            player.sendMessage("§cEste controlador climático pertence a outro jogador.");
            return;
        }

        boolean researched = api.canUseTechnology(player.getUniqueId(), "controlled_environment_farming");
        player.sendMessage("§bControlador climático MECHANIUM");
        player.sendMessage("§7Proprietário: §f" + player.getName());
        player.sendMessage("§7Raio de cultivo: §f" + radius() + " blocos");
        player.sendMessage("§7Consumo: §f" + energyPerGrowth() + " unidade(s) por crescimento adicional");
        player.sendMessage(researched
                ? "§aPesquisa de cultivo controlado desbloqueada."
                : "§cA pesquisa de cultivo controlado ainda não está desbloqueada.");
        player.sendMessage("§7Conecte uma bateria carregada por conduítes energéticos do mesmo proprietário.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        BlockState proposedState = event.getNewState();
        if (!(proposedState.getBlockData() instanceof Ageable proposedAge)) return;
        if (proposedAge.getAge() >= proposedAge.getMaximumAge()) return;

        Location cropLocation = event.getBlock().getLocation();
        Material expectedCropType = proposedState.getType();
        int expectedAge = proposedAge.getAge();
        // The Bukkit event may still be cancelled by another plugin at the same or a later
        // priority. Run after event dispatch and only boost the crop if its normal growth
        // state was actually committed to the world.
        Bukkit.getScheduler().runTask(plugin, () ->
                applyGrowthBonus(cropLocation, expectedCropType, expectedAge));
    }

    private void applyGrowthBonus(Location cropLocation, Material expectedCropType, int expectedAge) {
        World cropWorld = cropLocation.getWorld();
        if (cropWorld == null || !cropWorld.isChunkLoaded(
                cropLocation.getBlockX() >> 4, cropLocation.getBlockZ() >> 4)) return;

        Block cropBlock = cropWorld.getBlockAt(
                cropLocation.getBlockX(), cropLocation.getBlockY(), cropLocation.getBlockZ());
        if (cropBlock.getType() != expectedCropType) return;
        if (!(cropBlock.getBlockData() instanceof Ageable currentAge)) return;
        if (currentAge.getAge() != expectedAge) return;

        int boostedAge = ClimateGrowthPolicy.boostedAge(currentAge.getAge(), currentAge.getMaximumAge(), 1);
        if (boostedAge <= currentAge.getAge()) return;

        for (Location controller : nearbyControllers(cropLocation, radius())) {
            World world = controller.getWorld();
            if (world == null || !world.isChunkLoaded(
                    controller.getBlockX() >> 4, controller.getBlockZ() >> 4)) continue;

            Block controllerBlock = world.getBlockAt(
                    controller.getBlockX(), controller.getBlockY(), controller.getBlockZ());
            if (!isController(controllerBlock)) {
                unregisterController(controller);
                continue;
            }

            String ownerText = ownerId(controllerBlock);
            if (ownerText == null) continue;
            UUID owner;
            try {
                owner = UUID.fromString(ownerText);
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Controlador climático com proprietário inválido em "
                        + locationLabel(controllerBlock));
                continue;
            }
            if (!api.canUseTechnology(owner, "controlled_environment_farming")
                    || !api.canUseItem(owner, CONTROLLER_ID)) continue;
            if (energyGridManager == null
                    || !energyGridManager.consume(owner, controller, energyPerGrowth())) continue;

            currentAge.setAge(boostedAge);
            cropBlock.setBlockData(currentAge, false);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!isController(block)) return;
        String owner = ownerId(block);
        if (owner == null || !canManage(event.getPlayer(), owner)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cSomente o proprietário ou um administrador pode desmontar este controlador.");
            return;
        }

        ItemStack recovered = api.createItemStack(CONTROLLER_ID, 1)
                .orElseGet(() -> new ItemStack(Material.DISPENSER));
        Location drop = block.getLocation().add(0.5, 0.5, 0.5);
        Location controllerLocation = block.getLocation();
        event.setDropItems(false);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || !block.getType().isAir()) {
                if (isController(block)) registerController(block);
                else unregisterController(controllerLocation);
                return;
            }
            unregisterController(controllerLocation);
            block.getWorld().dropItemNaturally(drop, recovered);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isControllerAt(event.getSource().getLocation())
                || isControllerAt(event.getDestination().getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isController);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isController);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isController)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isController)) event.setCancelled(true);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        registerChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        UUID worldId = event.getWorld().getUID();
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        List<Location> unloaded = loadedControllers.values().stream()
                .filter(location -> location.getWorld() != null
                        && location.getWorld().getUID().equals(worldId)
                        && (location.getBlockX() >> 4) == chunkX
                        && (location.getBlockZ() >> 4) == chunkZ)
                .toList();
        unloaded.forEach(this::unregisterController);
    }

    private List<Location> nearbyControllers(Location crop, int radius) {
        if (crop.getWorld() == null) return List.of();
        int chunkRadius = (radius >> 4) + 1;
        int chunkX = crop.getBlockX() >> 4;
        int chunkZ = crop.getBlockZ() >> 4;
        UUID worldId = crop.getWorld().getUID();
        List<Location> candidates = new ArrayList<>();
        for (int x = chunkX - chunkRadius; x <= chunkX + chunkRadius; x++) {
            for (int z = chunkZ - chunkRadius; z <= chunkZ + chunkRadius; z++) {
                if (!crop.getWorld().isChunkLoaded(x, z)) continue;
                Map<String, Location> controllers = controllersByChunk.get(chunkKey(worldId, x, z));
                if (controllers == null) continue;
                for (Location controller : controllers.values()) {
                    if (ClimateGrowthPolicy.withinRange(controller.getBlockX(), controller.getBlockY(),
                            controller.getBlockZ(), crop.getBlockX(), crop.getBlockY(), crop.getBlockZ(), radius)) {
                        candidates.add(controller);
                    }
                }
            }
        }
        candidates.sort(Comparator.comparingDouble(controller -> controller.distanceSquared(crop)));
        return candidates;
    }

    private void registerChunk(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (state instanceof TileState tile
                    && CONTROLLER_ID.equals(tile.getPersistentDataContainer()
                    .get(itemIdKey, PersistentDataType.STRING))) {
                registerController(state.getBlock());
            }
        }
    }

    private void registerController(Block block) {
        if (!isController(block)) return;
        Location location = block.getLocation();
        String key = locationKey(location);
        loadedControllers.put(key, location);
        controllersByChunk.computeIfAbsent(
                chunkKey(block.getWorld().getUID(), block.getX() >> 4, block.getZ() >> 4),
                ignored -> new HashMap<>()).put(key, location);
    }

    private void unregisterController(Location location) {
        if (location == null || location.getWorld() == null) return;
        String key = locationKey(location);
        loadedControllers.remove(key);
        String chunkKey = chunkKey(location.getWorld().getUID(),
                location.getBlockX() >> 4, location.getBlockZ() >> 4);
        Map<String, Location> chunk = controllersByChunk.get(chunkKey);
        if (chunk != null) {
            chunk.remove(key);
            if (chunk.isEmpty()) controllersByChunk.remove(chunkKey);
        }
    }

    private boolean isController(Block block) {
        return block != null && block.getType() == Material.DISPENSER
                && block.getState() instanceof TileState tile
                && CONTROLLER_ID.equals(tile.getPersistentDataContainer()
                .get(itemIdKey, PersistentDataType.STRING));
    }

    private boolean isControllerAt(Location location) {
        if (location == null || location.getWorld() == null
                || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return false;
        }
        Block block = location.getWorld().getBlockAt(
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        return isController(block);
    }

    private String ownerId(Block block) {
        if (block == null || !(block.getState() instanceof TileState tile)) return null;
        return tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
    }

    private String itemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    private int radius() {
        return Math.max(1, Math.min(MAX_RADIUS,
                plugin.getConfig().getInt("greenhouse.growth-radius", DEFAULT_RADIUS)));
    }

    private int energyPerGrowth() {
        return Math.max(1, plugin.getConfig().getInt("greenhouse.energy-per-growth", 1));
    }

    private boolean canManage(Player player, String ownerId) {
        return player.getUniqueId().toString().equals(ownerId) || player.hasPermission("mechanium.admin");
    }

    private String locationKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":"
                + location.getBlockY() + ":" + location.getBlockZ();
    }

    private String chunkKey(UUID worldId, int chunkX, int chunkZ) {
        return worldId + ":" + chunkX + ":" + chunkZ;
    }

    private String locationLabel(Block block) {
        return block.getWorld().getName() + " " + block.getX() + "," + block.getY() + "," + block.getZ();
    }
}
