package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.MachineDefinition;
import dev.mechanium.api.model.MachineIngredient;
import dev.mechanium.api.model.MachineProduct;
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
 * Dispenser-hosted processor with six input slots and three output slots.
 * Machine identity, owner and progress live in block PDC; loaded machines are
 * scheduled through a bounded round-robin work queue.
 */
final class MechaniumMachineManager implements Listener {
    private static final int FIRST_INPUT_SLOT = 0;
    private static final int LAST_INPUT_SLOT = 5;
    private static final int FIRST_OUTPUT_SLOT = 6;
    private static final int LAST_OUTPUT_SLOT = 8;

    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final EnergyGridManager energyGridManager;
    private final NamespacedKey itemIdKey;
    private final NamespacedKey machineIdKey;
    private final NamespacedKey ownerKey;
    private final NamespacedKey progressKey;
    private final NamespacedKey emissionsKey;
    private final NamespacedKey wearKey;
    private final Map<String, Location> loadedMachines = new HashMap<>();
    private final MachineWorkQueue workQueue = new MachineWorkQueue();
    private BukkitTask ticker;

    MechaniumMachineManager(JavaPlugin plugin, MechaniumApi api, EnergyGridManager energyGridManager) {
        this.plugin = plugin;
        this.api = api;
        this.energyGridManager = energyGridManager;
        this.itemIdKey = Objects.requireNonNull(NamespacedKey.fromString("mechanium:item_id"));
        this.machineIdKey = new NamespacedKey(plugin, "machine_id");
        this.ownerKey = new NamespacedKey(plugin, "machine_owner");
        this.progressKey = new NamespacedKey(plugin, "machine_progress");
        this.emissionsKey = new NamespacedKey(plugin, "industrial_emissions");
        this.wearKey = new NamespacedKey(plugin, "machine_wear");
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
        workQueue.clear();
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
        dispenser.getPersistentDataContainer().set(wearKey, PersistentDataType.INTEGER, 0);
        if (!dispenser.update(true, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("Não foi possível salvar os dados da máquina.", NamedTextColor.RED));
            return;
        }
        registerLoadedMachine(key(block.getLocation()), block.getLocation());
        event.getPlayer().sendMessage(Component.text(definition.displayName() + " instalada. Slots 1–6: entradas; slots 7–9: saídas.", NamedTextColor.GREEN));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Optional<MachineState> machine = machineState(event.getClickedBlock());
        if (machine.isEmpty()) return;
        Player player = event.getPlayer();
        if (!canManage(player, machine.get().ownerId())) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Esta máquina pertence a outro jogador.", NamedTextColor.RED));
            return;
        }
        BlockState machineBlockState = event.getClickedBlock().getState();
        if (machineBlockState instanceof Dispenser dispenser) {
            int wear = dispenser.getPersistentDataContainer().getOrDefault(wearKey, PersistentDataType.INTEGER, 0);
            ItemStack hand = player.getInventory().getItemInMainHand();
            String heldItemId = itemId(hand);
            boolean maintenanceKit = "machine_maintenance_kit".equals(heldItemId)
                    && api.canUseItem(player.getUniqueId(), "machine_maintenance_kit");
            boolean ironRepair = hand.getType() == Material.IRON_INGOT;
            int ironCost = ironRepairCost();
            if (player.isSneaking() && wear > 0
                    && (maintenanceKit || (ironRepair && hand.getAmount() >= ironCost))) {
                int consumed = maintenanceKit ? 1 : ironCost;
                dispenser.getPersistentDataContainer().set(wearKey, PersistentDataType.INTEGER, 0);
                event.setCancelled(true);
                if (!dispenser.update(true, false)) {
                    player.sendMessage(Component.text(
                            "Não foi possível salvar a manutenção; nenhum item foi consumido.", NamedTextColor.RED));
                    return;
                }
                if (hand.getAmount() <= consumed) player.getInventory().setItemInMainHand(null);
                else hand.setAmount(hand.getAmount() - consumed);
                player.sendMessage(Component.text("Manutenção concluída. Desgaste zerado.", NamedTextColor.GREEN));
                return;
            }
            if (!wearEnabled()) {
                player.sendMessage(Component.text("Desgaste de máquinas desativado na configuração.", NamedTextColor.GRAY));
            } else {
                player.sendMessage(Component.text("Desgaste da máquina: " + wear + "/" + maximumWear()
                        + ". Agache com " + ironRepairCost() + " barra(s) de ferro ou um kit de manutenção desbloqueado.",
                        NamedTextColor.GRAY));
            }
            String machineId = dispenser.getPersistentDataContainer()
                    .get(machineIdKey, PersistentDataType.STRING);
            MachineDefinition definition = api.getMachine(machineId).orElse(null);
            if (definition != null) {
                String inputSummary = definition.inputs().stream()
                        .map(input -> input.amount() + "x " + input.itemId()).collect(java.util.stream.Collectors.joining(", "));
                String outputSummary = definition.outputs().stream()
                        .map(output -> output.amount() + "x " + output.itemId()).collect(java.util.stream.Collectors.joining(", "));
                player.sendMessage(Component.text("Entradas: " + inputSummary, NamedTextColor.GRAY));
                player.sendMessage(Component.text("Saídas: " + outputSummary, NamedTextColor.GRAY));
                player.sendMessage(Component.text("Tempo por operação: "
                        + definition.processingTicks() + " ticks.", NamedTextColor.GRAY));
                if (definition.energyCostPerOperation() > 0) {
                    player.sendMessage(Component.text("Consumo energético por operação: "
                            + definition.energyCostPerOperation() + " unidades.", NamedTextColor.AQUA));
                    player.sendMessage(Component.text("Conecte esta máquina a uma bateria carregada por conduítes energéticos do mesmo proprietário.",
                            NamedTextColor.GRAY));
                } else {
                    player.sendMessage(Component.text("Esta máquina não exige energia.", NamedTextColor.GRAY));
                }
            }
        }
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
                if (machineState(block).isPresent()) registerLoadedMachine(blockKey, block.getLocation());
                else unregisterLoadedMachine(blockKey);
                return;
            }
            unregisterLoadedMachine(blockKey);
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
            if (isInputSlot(rawSlot)) return;
            if (isOutputSlot(rawSlot) && event.getCurrentItem() != null
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
        if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize && !isInputSlot(slot))) {
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
        List<String> unloading = loadedMachines.entrySet().stream()
                .filter(entry -> {
                    Location location = entry.getValue();
                    return location.getWorld() != null && location.getWorld().getUID().equals(worldId)
                            && (location.getBlockX() >> 4) == chunkX
                            && (location.getBlockZ() >> 4) == chunkZ;
                })
                .map(Map.Entry::getKey)
                .toList();
        unloading.forEach(this::unregisterLoadedMachine);
    }

    private void registerLoadedMachine(String machineKey, Location location) {
        loadedMachines.put(machineKey, location);
        workQueue.add(machineKey);
    }

    private void unregisterLoadedMachine(String machineKey) {
        loadedMachines.remove(machineKey);
        workQueue.remove(machineKey);
    }

    private void registerChunkMachines(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof Dispenser dispenser)) continue;
            String machineId = dispenser.getPersistentDataContainer()
                    .get(machineIdKey, PersistentDataType.STRING);
            if (machineId == null || api.getMachine(machineId).isEmpty()) continue;
            registerLoadedMachine(key(state.getLocation()), state.getLocation());
        }
    }

    private void tickMachines() {
        int budget = Math.max(1, Math.min(1024,
                plugin.getConfig().getInt("machines.max-updates-per-cycle", 256)));
        for (String machineKey : workQueue.nextBatch(budget)) {
            Location location = loadedMachines.get(machineKey);
            if (location == null) continue;
            if (location.getWorld() == null || !location.getChunk().isLoaded()) {
                unregisterLoadedMachine(machineKey);
                continue;
            }
            BlockState state = location.getBlock().getState();
            if (!(state instanceof Dispenser dispenser)) {
                unregisterLoadedMachine(machineKey);
                continue;
            }
            try {
                process(dispenser);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(java.util.logging.Level.SEVERE,
                        "Falha ao processar máquina em " + location.getWorld().getName()
                                + " " + location.getBlockX() + "," + location.getBlockY()
                                + "," + location.getBlockZ() + "; ela será tentada novamente no próximo ciclo.",
                        exception);
            } finally {
                if (loadedMachines.containsKey(machineKey)) workQueue.add(machineKey);
            }
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
                || !api.canUseItem(owner, definition.machineItemId())) return;

        for (MachineIngredient ingredient : definition.inputs()) {
            if (ingredient.itemId().startsWith("mechanium:")
                    && !api.canUseItem(owner, ingredient.itemId().substring(10))) return;
        }
        for (MachineProduct product : definition.outputs()) {
            if (!api.canUseItem(owner, product.itemId())) return;
        }

        int wear = dispenser.getPersistentDataContainer().getOrDefault(wearKey, PersistentDataType.INTEGER, 0);
        if (wearEnabled() && wear >= maximumWear()) return;

        Inventory inventory = dispenser.getInventory();
        Map<Integer, Integer> inputPlan = planInputConsumption(inventory, definition.inputs());
        if (inputPlan == null) {
            setProgress(dispenser, 0);
            return;
        }

        int progress = Math.max(0, dispenser.getPersistentDataContainer()
                .getOrDefault(progressKey, PersistentDataType.INTEGER, 0));
        int progressPerCycle = api.canUseTechnology(owner, "industrial_efficiency") ? 40 : 20;
        progress = Math.min(definition.processingTicks(), progress + progressPerCycle);
        if (progress < definition.processingTicks()) {
            setProgress(dispenser, progress);
            return;
        }

        ItemStack[] outputPlan = planOutputs(inventory, definition.outputs());
        if (outputPlan == null) {
            // Preserve completed progress while output slots are full.
            setProgress(dispenser, definition.processingTicks());
            return;
        }

        int energyCost = definition.energyCostPerOperation();
        if (energyCost > 0 && (energyGridManager == null
                || !energyGridManager.consume(owner, dispenser.getLocation(), energyCost))) {
            setProgress(dispenser, progress);
            return;
        }

        applyInputConsumption(inventory, inputPlan);
        for (int i = 0; i < outputPlan.length; i++) {
            inventory.setItem(FIRST_OUTPUT_SLOT + i, outputPlan[i]);
        }
        setProgress(dispenser, 0);
        recordIndustrialImpact(dispenser.getWorld(), owner);
        if (wearEnabled()) {
            dispenser.getPersistentDataContainer().set(wearKey, PersistentDataType.INTEGER,
                    Math.min(maximumWear(), wear + wearPerOperation()));
        }
        dispenser.update(true, false);
    }

    private Map<Integer, Integer> planInputConsumption(Inventory inventory, List<MachineIngredient> ingredients) {
        Map<Integer, Integer> consumption = new java.util.LinkedHashMap<>();
        for (MachineIngredient ingredient : ingredients) {
            int remaining = ingredient.amount();
            for (int slot = FIRST_INPUT_SLOT; slot <= LAST_INPUT_SLOT && remaining > 0; slot++) {
                ItemStack candidate = inventory.getItem(slot);
                if (!matchesInput(candidate, ingredient.itemId())) continue;
                int alreadyReserved = consumption.getOrDefault(slot, 0);
                int available = candidate.getAmount() - alreadyReserved;
                if (available <= 0) continue;
                int taken = Math.min(available, remaining);
                consumption.put(slot, alreadyReserved + taken);
                remaining -= taken;
            }
            if (remaining > 0) return null;
        }
        return consumption;
    }

    private void applyInputConsumption(Inventory inventory, Map<Integer, Integer> consumption) {
        for (Map.Entry<Integer, Integer> entry : consumption.entrySet()) {
            ItemStack remaining = inventory.getItem(entry.getKey()).clone();
            int amount = remaining.getAmount() - entry.getValue();
            if (amount <= 0) inventory.setItem(entry.getKey(), null);
            else {
                remaining.setAmount(amount);
                inventory.setItem(entry.getKey(), remaining);
            }
        }
    }

    private ItemStack[] planOutputs(Inventory inventory, List<MachineProduct> products) {
        ItemStack[] planned = new ItemStack[LAST_OUTPUT_SLOT - FIRST_OUTPUT_SLOT + 1];
        for (int i = 0; i < planned.length; i++) {
            ItemStack current = inventory.getItem(FIRST_OUTPUT_SLOT + i);
            planned[i] = current == null || current.getType().isAir() ? null : current.clone();
        }

        for (MachineProduct product : products) {
            ItemStack template = api.createItemStack(product.itemId(), product.amount()).orElse(null);
            if (template == null) return null;
            int remaining = template.getAmount();

            for (int slot = 0; slot < planned.length && remaining > 0; slot++) {
                ItemStack current = planned[slot];
                if (current == null || !current.isSimilar(template)) continue;
                int available = current.getMaxStackSize() - current.getAmount();
                if (available <= 0) continue;
                int inserted = Math.min(available, remaining);
                current.setAmount(current.getAmount() + inserted);
                remaining -= inserted;
            }

            for (int slot = 0; slot < planned.length && remaining > 0; slot++) {
                if (planned[slot] != null) continue;
                int inserted = Math.min(template.getMaxStackSize(), remaining);
                ItemStack stack = template.clone();
                stack.setAmount(inserted);
                planned[slot] = stack;
                remaining -= inserted;
            }

            if (remaining > 0) return null;
        }
        return planned;
    }

    private boolean isInputSlot(int slot) {
        return slot >= FIRST_INPUT_SLOT && slot <= LAST_INPUT_SLOT;
    }

    private boolean isOutputSlot(int slot) {
        return slot >= FIRST_OUTPUT_SLOT && slot <= LAST_OUTPUT_SLOT;
    }

    private boolean wearEnabled() {
        return plugin.getConfig().getBoolean("machines.wear.enabled", true);
    }

    private int maximumWear() {
        return Math.max(1, plugin.getConfig().getInt("machines.wear.max-cycles", 100));
    }

    private int wearPerOperation() {
        int base = Math.max(0, plugin.getConfig().getInt("machines.wear.cycles-per-operation", 1));
        int multiplier = plugin.getConfig().getBoolean("machines.hardcore.enabled", false)
                ? Math.max(1, plugin.getConfig().getInt("machines.hardcore.wear-multiplier", 2)) : 1;
        return Math.max(0, base * multiplier);
    }

    private int ironRepairCost() {
        return plugin.getConfig().getBoolean("machines.hardcore.enabled", false)
                ? Math.max(1, plugin.getConfig().getInt("machines.hardcore.repair-iron-ingots", 2)) : 1;
    }

    private void recordIndustrialImpact(World world, UUID owner) {
        // Emissions are an abstract gameplay metric, not a real-world pollution simulation.
        if (api.canUseTechnology(owner, "emissions_control")) return;
        long current = world.getPersistentDataContainer()
                .getOrDefault(emissionsKey, PersistentDataType.LONG, 0L);
        world.getPersistentDataContainer().set(emissionsKey, PersistentDataType.LONG,
                current == Long.MAX_VALUE ? current : current + 1L);
    }

    long industrialImpact(World world) {
        if (world == null) return 0L;
        return world.getPersistentDataContainer()
                .getOrDefault(emissionsKey, PersistentDataType.LONG, 0L);
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
