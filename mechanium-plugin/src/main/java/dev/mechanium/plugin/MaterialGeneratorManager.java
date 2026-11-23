package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

/** Bounded passive generators. Output is inserted only into a chest directly above the machine. */
final class MaterialGeneratorManager implements Listener {
    private static final Map<String, Material> OUTPUTS = Map.of(
            "material_generator_cobblestone", Material.COBBLESTONE,
            "material_generator_stone", Material.STONE,
            "material_generator_smooth_stone", Material.SMOOTH_STONE,
            "material_generator_gravel", Material.GRAVEL,
            "material_generator_sand", Material.SAND,
            "material_generator_glass", Material.GLASS,
            "material_generator_netherrack", Material.NETHERRACK,
            "material_generator_soul_sand", Material.SOUL_SAND
    );
    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final NamespacedKey itemIdKey = NamespacedKey.fromString("mechanium:item_id");
    private final NamespacedKey ownerKey;
    private final NamespacedKey progressKey;
    private BukkitTask task;

    MaterialGeneratorManager(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
        this.ownerKey = new NamespacedKey(plugin, "material_generator_owner");
        this.progressKey = new NamespacedKey(plugin, "material_generator_progress");
    }

    void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    void stop() {
        if (task != null) { task.cancel(); task = null; }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        String id = itemId(event.getItemInHand());
        if (id == null || !OUTPUTS.containsKey(id)) return;
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.DISPENSER || !(block.getState() instanceof TileState tile)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cO gerador de materiais precisa ser colocado como dispenser.");
            return;
        }
        tile.getPersistentDataContainer().set(itemIdKey, PersistentDataType.STRING, id);
        tile.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, event.getPlayer().getUniqueId().toString());
        tile.getPersistentDataContainer().set(progressKey, PersistentDataType.INTEGER, 0);
        if (!tile.update(true, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNão foi possível registrar o gerador.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        String id = blockItemId(event.getClickedBlock());
        if (id == null || !OUTPUTS.containsKey(id)) return;
        event.setCancelled(true);
        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof TileState tile)) return;
        int progress = tile.getPersistentDataContainer().getOrDefault(progressKey, PersistentDataType.INTEGER, 0);
        int interval = intervalTicks();
        event.getPlayer().sendMessage("§eGerador: §f" + OUTPUTS.get(id).name()
                + " §7— progresso §f" + Math.min(progress, interval) + "§7/§f" + interval
                + " ticks. Baú necessário diretamente acima.");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        String id = blockItemId(event.getBlock());
        if (id == null || !OUTPUTS.containsKey(id)) return;
        BlockState state = event.getBlock().getState();
        if (!(state instanceof TileState tile)) return;
        String owner = tile.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (owner != null && !owner.equals(event.getPlayer().getUniqueId().toString())
                && !event.getPlayer().hasPermission("mechanium.admin")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cSomente o proprietário ou um administrador pode desmontar este gerador.");
            return;
        }
        ItemStack recovered = api.createItemStack(id, 1).orElseGet(() -> new ItemStack(Material.DISPENSER));
        Block block = event.getBlock();
        var drop = block.getLocation().add(0.5, 0.5, 0.5);
        event.setDropItems(false);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (block.getType().isAir()) block.getWorld().dropItemNaturally(drop, recovered);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) { event.blockList().removeIf(this::isGenerator); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) { event.blockList().removeIf(this::isGenerator); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isGenerator)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().stream().anyMatch(this::isGenerator)) event.setCancelled(true);
    }

    private void tick() {
        int interval = intervalTicks();
        for (var world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState state : chunk.getTileEntities()) {
                    if (!(state instanceof TileState tile)) continue;
                    String id = tile.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
                    if (id == null) continue;
                    Material output = OUTPUTS.get(id);
                    if (output == null || state.getType() != Material.DISPENSER) continue;
                    Block chestBlock = state.getBlock().getRelative(BlockFace.UP);
                    if (chestBlock.getType() != Material.CHEST && chestBlock.getType() != Material.TRAPPED_CHEST) continue;
                    BlockState chestState = chestBlock.getState();
                    if (!(chestState instanceof InventoryHolder holder)) continue;
                    Inventory inventory = holder.getInventory();
                    // addItem can still merge into a matching partial stack even when every slot is occupied.
                    int progress = tile.getPersistentDataContainer().getOrDefault(progressKey, PersistentDataType.INTEGER, 0) + 20;
                    if (progress >= interval) {
                        if (inventory.addItem(new ItemStack(output, 1)).isEmpty()) progress = 0;
                        else progress = Math.max(0, interval - 20);
                    }
                    tile.getPersistentDataContainer().set(progressKey, PersistentDataType.INTEGER, progress);
                    tile.update(true, false);
                }
            }
        }
    }

    private int intervalTicks() {
        return Math.max(20, plugin.getConfig().getInt("modules.material-generators.interval-ticks", 200));
    }

    private boolean isGenerator(Block block) { String id = blockItemId(block); return id != null && OUTPUTS.containsKey(id); }

    private String blockItemId(Block block) {
        if (block == null || !(block.getState() instanceof TileState tile)) return null;
        return tile.getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }

    private String itemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(itemIdKey, PersistentDataType.STRING);
    }
}
