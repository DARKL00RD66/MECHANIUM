package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.MachineDefinition;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;

/**
 * First playable machine runtime: a dispenser-hosted processor with one input slot
 * and one output slot. Machine identity, owner and progress live in block PDC.
 */
final class MechaniumMachineManager implements Listener {
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 8;

    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey machineIdKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey progressKey;
    private final Map<String, Location> loadedMachines = new HashMap<>();
    private BukkitTask ticker;

    MechaniumMachineManager(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
        this.itemIdKey = Objects.requireNonNull(NamespacedKey.fromString("mechanium:item_id"));
        this.machineIdKey = new NamespacedKey(plugin, "machine_id");
        this.ownerKey = new NamespacedKey(plugin, "machine_owner");
        this.progressKey = new NamespacedKey(plugin, "machine_progress");
    }

    void start() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) registerChunkMachines(chunk);
        }
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, this::tickMachines, 20L, 20L);
    }

    void stop() {
        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }
        loadedMachines.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        String itemId = itemId(event.getItemInHand());
        if (itemId == null) return;
        MachineDefinition definition = api.getMachines().stream()
                .filter(machine -> machine.machineItemId().equals(itemId)).findFirst().orElse(null);
        if (definition == null) return;

        Block block = event.getBlockPlaced();
        if (block.getType() != Material.valueOf(definition.blockMaterial())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Bloco incompatível com esta máquina.", NamedTextColor.RED));
            return;
        }
        BlockState state = block.getState();
        if (!(state instanceof Dispenser dispenser)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Não foi possível inicializar esta máquina.", NamedTextColor.RED));
            return;
        }

        dispenser.getPersistentDataContainer().set(machineIdKey, PersistentDataType.STRING, definition.id());
        dispenser.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                event.getPlayer().getUniqueId().toString());
        dispenser.getPersistentDataContainer().set(progressKey, PersistentDataType.INTEGER, 0);
        if (!dispenser.update(true, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Não foi possível salvar os dados da máquina.", NamedTextColor.RED));
            return;
        }
        loadedMachines.put(key(block.getLocation()), block.getLocation());
        event.getPlayer().sendMessage(Component.text(definition.displayName() + " instalada. Entrada no primeiro espaço; saída no último.", NamedTextColor.GREEN));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Optional<MachineState> machine = machineState(event.getClickedBlock());
        if (machine.isEmpty()) return;
        if (canManage(event.getPlayer(), machine.get().ownerId())) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.text("Esta máquina pertence a outro jogador.", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Optional<MachineState> machine = machineState(event.getBlock());
        if (machine.isEmpty()) return;

        Player player = event.getPlayer();
        MachineState details = machine.get();
        if (!canManage(player, details.ownerId())) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Somente o proprietário ou um administrador pode desmontar esta máquina.", NamedTextColor.RED));
            return;
        }

        Block block = event.getBlock();
        List<ItemStack> contents = new ArrayList<>();
        BlockState state = block.getState();
        if (state instanceof Dispenser dispenser) {
            for (ItemStack content : dispenser.getInventory().getContents()) {
                if (content != null && !content.getType().isAir()) contents.add(content.clone());
            }
        }
        ItemStack recovered = api.getMachine(details.machineId())
                .flatMap(definition -> api.createItemStack(definition.machineItemId(), 1))
                .orElseGet(() -> new ItemStack(Material.DISPENSER));
        Location dropLocation = block.getLocation().add(0.5, 0.5, 0.5);
        String blockKey = key(block.getLocation());
        event.setDropItems(false);
        // Defer drops until all other BlockBreakEvent handlers have had a chance to cancel.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.isCancelled() || !block.getType().isAir()) {
                if (machineState(block).isPresent()) loadedMachines.put(blockKey, block.getLocation());
                else loadedMachines.remove(blockKey);
                return;
            }
            loadedMachines.remove(blockKey);
            for (ItemStack content : contents) {
                block.getWorld().dropItemNaturally(dropLocation, content);
            }
            block.getWorld().dropItemNaturally(dropLocation, recovered);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        Location location = event.getView().getTopInventory().getLocation();
        if (location == null || machineState(location.getBlock()).isEmpty()) return;

        int topSize = event.getView().getTopInventory().getSize();
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0) return;
        if (event.isShiftClick() && rawSlot >= topSize) {
            event.setCancelled(true);
            return;
        }
        if (rawSlot < topSize) {
            if (rawSlot == INPUT_SLOT) return;
            if (rawSlot == OUTPUT_SLOT && event.getCurrentItem() != null
                    && !event.getCurrentItem().getType().isAir()
                    && (event.getCursor() == null || event.getCursor().getType().isAir())
                    && (event.getClick().isLeftClick() || event.getClick().isRightClick()
                        || event.isShiftClick())) return;
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Location location = event.getView().getTopInventory().getLocation();
        if (location == null || machineState(location.getBlock()).isEmpty()) return;
        int topSize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize && slot != INPUT_SLOT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isMachineInventory(event.getSource().getLocation())
                || isMachineInventory(event.getDestination().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        if (machineState(event.getBlock()).isPresent()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        event.blockList().removeIf(block -> machineState(block).isPresent());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> machineState(block).isPresent());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> machineState(block).isPresent())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(block -> machineState(block).isPresent())) event.setCancelled(true);
    }

    @EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        registerChunkMachines(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(org.bukkit.event.world.ChunkUnloadEvent event) {
        int chunkX = event.getChunk().getX();
        int chunkZ = event.getChunk().getZ();
        UUID worldId = event.getWorld().getUID();
        loadedMachines.entrySet().removeIf(entry -> {
            Location location = entry.getValue();
            return location.getWorld() != null && location.getWorld().getUID().equals(worldId)
                    && (location.getBlockX() >> 4) == chunkX && (location.getBlockZ() >> 4) == chunkZ;
        });
    }

    private void registerChunkMachines(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof Dispenser dispenser)) continue;
            String machineId = dispenser.getPersistentDataContainer()
                    .get(machineIdKey, PersistentDataType.STRING);
            if (machineId == null || api.getMachine(machineId).isEmpty()) continue;
            loadedMachines.put(key(state.getLocation()), state.getLocation());
        }
    }

    private void tickMachines() {
        for (Location location : new ArrayList<>(loadedMachines.values())) {
            if (location.getWorld() == null || !location.getChunk().isLoaded()) {
                loadedMachines.remove(key(location));
                continue;
            }
            BlockState state = location.getBlock().getState();
            if (!(state instanceof Dispenser dispenser)) {
                loadedMachines.remove(key(location));
                continue;
            }
            process(dispenser);
        }
    }

    private void process(Dispenser dispenser) {
        String machineId = dispenser.getPersistentDataContainer().get(machineIdKey, PersistentDataType.STRING);
        String ownerText = dispenser.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (machineId == null || ownerText == null) return;

        MachineDefinition definition = api.getMachine(machineId).orElse(null);
        if (definition == null) return;
        UUID owner;
        try {
            owner = UUID.fromString(ownerText);
        } catch (IllegalArgumentException exception) {
            return;
        }
        if (!api.canUseTechnology(owner, definition.requiredTechnologyId())
                || !api.canUseItem(owner, definition.machineItemId())
                || !api.canUseItem(owner, definition.outputItemId())) return;

        Inventory inventory = dispenser.getInventory();
        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (!matchesInput(input, definition.inputId()) || input.getAmount() < definition.inputAmount()) {
            setProgress(dispenser, 0);
            return;
        }
        if (definition.inputId().startsWith("mechanium:")
                && !api.canUseItem(owner, definition.inputId().substring(10))) return;

        int progress = Math.max(0, dispenser.getPersistentDataContainer()
                .getOrDefault(progressKey, PersistentDataType.INTEGER, 0));
        progress = Math.min(definition.processingTicks(), progress + 20);
        if (progress < definition.processingTicks()) {
            setProgress(dispenser, progress);
            return;
        }

        ItemStack output = api.createItemStack(definition.outputItemId(), definition.outputAmount()).orElse(null);
        if (output == null) return;
        ItemStack existing = inventory.getItem(OUTPUT_SLOT);
        if (existing != null && !existing.getType().isAir()
                && (!existing.isSimilar(output)
                    || existing.getAmount() + output.getAmount() > existing.getMaxStackSize())) {
            setProgress(dispenser, definition.processingTicks());
            return;
        }

        int remaining = input.getAmount() - definition.inputAmount();
        if (remaining < 0) {
            setProgress(dispenser, 0);
            return;
        }
        if (remaining == 0) inventory.setItem(INPUT_SLOT, null);
        else {
            input.setAmount(remaining);
            inventory.setItem(INPUT_SLOT, input);
        }

        if (existing == null || existing.getType().isAir()) inventory.setItem(OUTPUT_SLOT, output);
        else {
            existing.setAmount(existing.getAmount() + output.getAmount());
            inventory.setItem(OUTPUT_SLOT, existing);
        }
        setProgress(dispenser, 0);
    }

    private boolean matchesInput(ItemStack input, String inputId) {
        if (input == null || input.getType().isAir()) return false;
        if (inputId.startsWith("mechanium:")) return inputId.substring(10).equals(itemId(input));
        return input.getType() == Material.valueOf(inputId);
    }

    private void setProgress(Dispenser dispenser, int progress) {
        dispenser.getPersistentDataContainer().set(progressKey, PersistentDataType.INTEGER, progress);
        dispenser.update(true, false);
    }

    private String itemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    private Optional<MachineState> machineState(Block block) {
        if (block == null || block.getType() != Material.DISPENSER) return Optional.empty();
        BlockState state = block.getState();
        if (!(state instanceof Dispenser dispenser)) return Optional.empty();
        String machineId = dispenser.getPersistentDataContainer().get(machineIdKey, PersistentDataType.STRING);
        String owner = dispenser.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (machineId == null || owner == null) return Optional.empty();
        return Optional.of(new MachineState(machineId, owner));
    }

    private boolean isMachineInventory(Location location) {
        return location != null && machineState(location.getBlock()).isPresent();
    }

    private boolean canManage(Player player, String ownerId) {
        return player.getUniqueId().toString().equals(ownerId) || player.hasPermission("mechanium.admin");
    }

    private String key(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":"
                + location.getBlockY() + ":" + location.getBlockZ();
    }

    private record MachineState(String machineId, String ownerId) { }
}
