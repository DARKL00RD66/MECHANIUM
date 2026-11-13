package dev.mechanium.plugin;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Portable 27-slot storage; contents are serialized into the unique item PDC. */
final class PortableStorageListener implements Listener {
    private final NamespacedKey itemIdKey = NamespacedKey.fromString("mechanium:item_id");
    private final NamespacedKey contentsKey;
    private final Map<UUID, PortableHolder> sessions = new HashMap<>();

    PortableStorageListener(JavaPlugin plugin) {
        this.contentsKey = new NamespacedKey(plugin, "portable_storage_contents");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND
                || (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR
                    && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) return;
        ItemStack held = event.getItem();
        if (!isPortable(held)) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (sessions.containsKey(player.getUniqueId())) return;
        int slot = player.getInventory().getHeldItemSlot();
        PortableHolder holder = new PortableHolder(player.getUniqueId(), slot);
        Inventory inventory = Bukkit.createInventory(holder, 27, "MECHANIUM • Armazém portátil");
        holder.inventory = inventory;
        load(held, inventory);
        sessions.put(player.getUniqueId(), holder);
        player.openInventory(inventory);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PortableHolder)) return;
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (isPortable(current) || isPortable(cursor) || event.isShiftClick()) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof PortableHolder)) return;
        if (isPortable(event.getOldCursor())) event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof PortableHolder holder)) return;
        sessions.remove(holder.playerId);
        Player player = Bukkit.getPlayer(holder.playerId);
        if (player == null) return;
        ItemStack storage = player.getInventory().getItem(holder.slot);
        if (!isPortable(storage)) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                if (isPortable(player.getInventory().getItem(i))) {
                    storage = player.getInventory().getItem(i);
                    break;
                }
            }
        }
        if (!isPortable(storage)) {
            player.sendMessage("§cNão foi possível localizar seu armazém portátil; mantenha o item no inventário ao abrir.");
            return;
        }
        ItemMeta meta = storage.getItemMeta();
        meta.getPersistentDataContainer().set(contentsKey, PersistentDataType.BYTE_ARRAY, serialize(holder.inventory.getContents()));
        storage.setItemMeta(meta);
        player.updateInventory();
    }

    private void load(ItemStack storage, Inventory target) {
        if (storage == null || !storage.hasItemMeta()) return;
        byte[] bytes = storage.getItemMeta().getPersistentDataContainer().get(contentsKey, PersistentDataType.BYTE_ARRAY);
        if (bytes == null || bytes.length == 0) return;
        try (BukkitObjectInputStream input = new BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            Object value = input.readObject();
            if (value instanceof ItemStack[] contents && contents.length == target.getSize()) target.setContents(contents);
        } catch (IOException | ClassNotFoundException exception) {
            Bukkit.getLogger().warning("Não foi possível carregar um armazém portátil: " + exception.getMessage());
        }
    }

    private byte[] serialize(ItemStack[] contents) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
            output.writeObject(contents);
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Falha ao salvar armazém portátil", exception);
        }
    }

    private boolean isPortable(ItemStack item) {
        return item != null && item.hasItemMeta()
                && "portable_storage".equals(item.getItemMeta().getPersistentDataContainer()
                    .get(itemIdKey, PersistentDataType.STRING));
    }

    private static final class PortableHolder implements InventoryHolder {
        private final UUID playerId;
        private final int slot;
        private Inventory inventory;
        private PortableHolder(UUID playerId, int slot) { this.playerId = playerId; this.slot = slot; }
        @Override public Inventory getInventory() { return inventory; }
    }
}