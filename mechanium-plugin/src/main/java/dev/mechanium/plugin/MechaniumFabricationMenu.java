package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.MachineDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.function.Consumer;

/**
 * Click-to-fabricate catalog. Ingredient checks and consumption are performed server-side,
 * including exact MECHANIUM item IDs stored in the item's PDC.
 */
final class MechaniumFabricationMenu implements Listener {
    private static final int[] SLOTS = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
    private static final NamespacedKey ITEM_ID = new NamespacedKey("mechanium", "item_id");
    private final MechaniumPlugin plugin;
    private final MechaniumApi api;

    MechaniumFabricationMenu(MechaniumPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    void open(Player player) { open(player, 0); }

    private void open(Player player, int requestedPage) {
        List<RecipeDefinition> recipes = api.getRecipes().stream()
                .filter(recipe -> api.getItem(recipe.resultItemId()).isPresent())
                .sorted(Comparator
                        .comparing((RecipeDefinition r) -> api.getItem(r.resultItemId())
                                .map(ItemDefinition::categoryId).orElse("misc"))
                        .thenComparing(r -> api.getItem(r.resultItemId())
                                .map(ItemDefinition::displayName).orElse(r.resultItemId()),
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
        int pages = Math.max(1, (recipes.size() + SLOTS.length - 1) / SLOTS.length);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, 54, Component.text("MECHANIUM • Fabricação"));
        holder.inventory = inv;
        ItemStack frame = named(Material.BLACK_STAINED_GLASS_PANE, "§0 ", List.of());
        for (int slot = 0; slot < 54; slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) inv.setItem(slot, frame);
        }
        inv.setItem(4, named(Material.KNOWLEDGE_BOOK, "§6§lCATÁLOGO DE FABRICAÇÃO",
                List.of("§7Pontos de pesquisa: §b" + api.getResearchPoints(player.getUniqueId()),
                        "§7Página: §f" + (page + 1) + "/" + pages,
                        "§8Clique em um item para fabricar.",
                        "§8Os ingredientes são consumidos do inventário.")));
        int start = page * SLOTS.length;
        int count = Math.min(SLOTS.length, recipes.size() - start);
        for (int i = 0; i < count; i++) {
            RecipeDefinition recipe = recipes.get(start + i);
            ItemDefinition item = api.getItem(recipe.resultItemId()).orElseThrow();
            boolean machine = api.getMachines().stream().anyMatch(m -> m.machineItemId().equals(item.id()));
            Material icon = Material.matchMaterial(item.material());
            if (icon == null || !icon.isItem()) icon = Material.BARRIER;
            List<String> lore = new ArrayList<>();
            lore.add("§7Categoria: §f" + item.categoryId());
            lore.add("§7Tipo: " + (machine ? "§6Máquina" : "§bComponente/item"));
            lore.add("§7Receita: §f" + recipe.id());
            lore.add("§7Tecnologia: §f" + Objects.toString(recipe.requiredTechnologyId(), "Nenhuma"));
            lore.add("§7Ingredientes:");
            ingredientCounts(recipe).forEach((ingredient, amount) ->
                    lore.add(" §8• §f" + ingredientLabel(ingredient) + " x" + amount));
            lore.add(api.canCraft(player.getUniqueId(), recipe.id())
                    && api.canUseItem(player.getUniqueId(), item.id())
                    ? "§aClique para fabricar."
                    : "§cTecnologia ainda bloqueada.");
            int slot = SLOTS[i];
            inv.setItem(slot, named(icon, "§e" + item.displayName(), lore));
            holder.actions.put(slot, p -> {
                craft(p, recipe);
                open(p, page);
            });
        }
        if (page > 0) holder.actions.put(45, p -> open(p, page - 1));
        if (page > 0) inv.setItem(45, named(Material.ARROW, "§ePágina anterior", List.of()));
        if (page + 1 < pages) holder.actions.put(53, p -> open(p, page + 1));
        if (page + 1 < pages) inv.setItem(53, named(Material.ARROW, "§ePróxima página", List.of()));
        holder.actions.put(49, Player::closeInventory);
        inv.setItem(49, named(Material.BARRIER, "§cFechar", List.of()));
        player.openInventory(inv);
    }

    private void craft(Player player, RecipeDefinition recipe) {
        ItemDefinition resultDefinition = api.getItem(recipe.resultItemId()).orElse(null);
        if (resultDefinition == null) return;
        if (!api.canCraft(player.getUniqueId(), recipe.id())
                || !api.canUseItem(player.getUniqueId(), resultDefinition.id())) {
            player.sendMessage("§cVocê ainda não desbloqueou a tecnologia necessária para fabricar isso.");
            return;
        }
        Map<String, Integer> required = ingredientCounts(recipe);
        for (var entry : required.entrySet()) {
            if (count(player, entry.getKey()) < entry.getValue()) {
                player.sendMessage("§cFaltam ingredientes: §f" + ingredientLabel(entry.getKey())
                        + " x" + entry.getValue() + "§c.");
                return;
            }
        }
        ItemStack result = api.createItemStack(recipe.resultItemId(), recipe.resultAmount()).orElse(null);
        if (result == null) {
            player.sendMessage("§cO resultado desta receita não pôde ser criado.");
            return;
        }
        Inventory simulation = Bukkit.createInventory(null, 36);
        simulation.setStorageContents(player.getInventory().getStorageContents());
        if (!simulation.addItem(result.clone()).isEmpty()) {
            player.sendMessage("§cLibere espaço no inventário antes de fabricar.");
            return;
        }
        for (var entry : required.entrySet()) remove(player, entry.getKey(), entry.getValue());
        player.getInventory().addItem(result);
        player.sendMessage("§aFabricado: §f" + resultDefinition.displayName() + " §7x" + recipe.resultAmount());
    }

    private Map<String, Integer> ingredientCounts(RecipeDefinition recipe) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (recipe.type() == RecipeDefinition.Type.SHAPELESS) {
            recipe.ingredients().values().forEach(value -> counts.merge(value, 1, Integer::sum));
        } else {
            recipe.shape().forEach(row -> row.chars().filter(c -> c != ' ').forEach(c ->
                    counts.merge(recipe.ingredients().get((char)c), 1, Integer::sum)));
        }
        return counts;
    }

    private int count(Player player, String ingredient) {
        int total = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (matches(stack, ingredient)) total += stack.getAmount();
        }
        return total;
    }

    private void remove(Player player, String ingredient, int amount) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (!matches(stack, ingredient)) continue;
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) contents[i] = null;
            remaining -= take;
        }
        player.getInventory().setStorageContents(contents);
    }

    private boolean matches(ItemStack stack, String ingredient) {
        if (stack == null || stack.getType().isAir()) return false;
        if (ingredient.startsWith("mechanium:")) {
            if (!stack.hasItemMeta()) return false;
            String id = stack.getItemMeta().getPersistentDataContainer().get(ITEM_ID, PersistentDataType.STRING);
            return ingredient.substring(10).equals(id);
        }
        Material material = Material.matchMaterial(ingredient);
        return material != null && stack.getType() == material;
    }

    private String ingredientLabel(String ingredient) {
        if (ingredient.startsWith("mechanium:")) {
            return api.getItem(ingredient.substring(10)).map(ItemDefinition::displayName)
                    .orElse(ingredient.substring(10));
        }
        return ingredient.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private ItemStack named(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        Consumer<Player> action = holder.actions.get(event.getSlot());
        if (action != null) action.accept(player);
    }

    private static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
        @Override public Inventory getInventory() { return inventory; }
    }
}
