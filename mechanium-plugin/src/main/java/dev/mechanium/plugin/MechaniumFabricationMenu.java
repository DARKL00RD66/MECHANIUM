package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.MachineDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Consumer;

/**
 * Categorized click-to-fabricate catalog. It validates all ingredients and inventory capacity
 * against a temporary inventory before committing consumption and output in one operation.
 */
final class MechaniumFabricationMenu implements Listener {
    private static final int[] SLOTS = {
            10,11,12,13,14,15,16,19,20,21,22,23,24,25,
            28,29,30,31,32,33,34,37,38,39,40,41,42,43
    };
    private static final NamespacedKey ITEM_ID = new NamespacedKey("mechanium", "item_id");

    private final JavaPlugin plugin;
    private final MechaniumApi api;

    MechaniumFabricationMenu(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    void open(Player player) {
        open(player, 0, ItemFilter.ALL, 0);
    }

    private void open(Player player, int requestedPage, ItemFilter filter, int requestedCategory) {
        List<ContentCategory> categories = api.getCategories().stream()
                .sorted(Comparator.comparing(ContentCategory::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int categoryIndex = Math.max(0, Math.min(requestedCategory, categories.size()));
        String categoryId = categoryIndex == 0 ? null : categories.get(categoryIndex - 1).id();
        Map<String, MachineDefinition> machineByItem = new HashMap<>();
        api.getMachines().forEach(machine -> machineByItem.put(machine.machineItemId(), machine));

        List<RecipeDefinition> recipes = api.getRecipes().stream()
                .filter(recipe -> api.getItem(recipe.resultItemId()).isPresent())
                .filter(recipe -> {
                    boolean machine = machineByItem.containsKey(recipe.resultItemId());
                    return switch (filter) {
                        case ALL -> true;
                        case MACHINES -> machine;
                        case ITEMS -> !machine;
                    };
                })
                .filter(recipe -> categoryId == null || api.getItem(recipe.resultItemId())
                        .map(item -> categoryId.equals(item.categoryId())).orElse(false))
                .sorted(Comparator
                        .comparing((RecipeDefinition recipe) -> api.getItem(recipe.resultItemId())
                                .map(ItemDefinition::categoryId).orElse("misc"))
                        .thenComparing(recipe -> api.getItem(recipe.resultItemId())
                                .map(ItemDefinition::displayName).orElse(recipe.resultItemId()),
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

        String categoryName = categoryId == null ? "Todas" : categories.get(categoryIndex - 1).displayName();
        inv.setItem(4, named(Material.KNOWLEDGE_BOOK, "§6§lCATÁLOGO DE FABRICAÇÃO", List.of(
                "§7Pontos de pesquisa: §b" + api.getResearchPoints(player.getUniqueId()),
                "§7Tipo: §f" + filter.label,
                "§7Categoria: §f" + categoryName,
                "§7Resultados: §f" + recipes.size(),
                "§7Página: §f" + (page + 1) + "/" + pages,
                "§8Clique no resultado para fabricar.",
                "§8Os ingredientes são consumidos do inventário."
        )));

        int start = page * SLOTS.length;
        int count = Math.min(SLOTS.length, recipes.size() - start);
        for (int i = 0; i < count; i++) {
            RecipeDefinition recipe = recipes.get(start + i);
            ItemDefinition item = api.getItem(recipe.resultItemId()).orElseThrow();
            MachineDefinition machine = machineByItem.get(item.id());
            boolean unlocked = api.canCraft(player.getUniqueId(), recipe.id())
                    && api.canUseItem(player.getUniqueId(), item.id());
            boolean ingredientsAvailable = hasIngredients(player, recipe);
            Material icon = Material.matchMaterial(item.material());
            if (icon == null || !icon.isItem()) icon = Material.BARRIER;

            List<String> lore = new ArrayList<>();
            String displayCategory = api.getCategory(item.categoryId())
                    .map(ContentCategory::displayName).orElse(item.categoryId());
            lore.add("§7Categoria: §f" + displayCategory);
            lore.add("§7Classificação: " + (machine == null ? "§bItem / componente" : "§6Máquina"));
            lore.add("§7ID: §8" + item.id());
            lore.add("§7Receita: §8" + recipe.id());

            String requiredTech = recipe.requiredTechnologyId() != null
                    ? recipe.requiredTechnologyId() : item.requiredTechnologyId();
            if (requiredTech != null) {
                TechnologyDefinition technology = api.getTechnology(requiredTech).orElse(null);
                lore.add("§7Pesquisa: §d" + (technology == null ? requiredTech : technology.displayName()));
            } else {
                lore.add("§7Pesquisa: §aNenhuma");
            }

            if (machine != null) {
                lore.add("§7Processo: §f" + ingredientLabel(machine.inputId()) + " x" + machine.inputAmount()
                        + " → " + api.getItem(machine.outputItemId())
                        .map(ItemDefinition::displayName).orElse(machine.outputItemId()));
                lore.add("§7Duração: §f" + (machine.processingTicks() / 20) + " segundos por ciclo");
            }

            lore.add("§7Ingredientes necessários:");
            ingredientCounts(recipe).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> lore.add(" §8• §f" + ingredientLabel(entry.getKey()) + " x" + entry.getValue()));

            if (!unlocked) lore.add("§cBloqueado: conclua a pesquisa indicada.");
            else if (!ingredientsAvailable) lore.add("§eFaltam ingredientes no seu inventário.");
            else lore.add("§aClique para fabricar e consumir os ingredientes.");

            int slot = SLOTS[i];
            inv.setItem(slot, named(icon, (machine == null ? "§b" : "§6") + item.displayName(), lore));
            holder.actions.put(slot, p -> {
                craft(p, recipe);
                open(p, page, filter, categoryIndex);
            });
        }

        if (recipes.isEmpty()) {
            inv.setItem(22, named(Material.BARRIER, "§cNenhum resultado nesta seleção", List.of(
                    "§7Mude o tipo de conteúdo ou a categoria.",
                    "§7Alguns conteúdos podem estar em módulos desativados."
            )));
        }

        if (page > 0) {
            inv.setItem(45, named(Material.ARROW, "§ePágina anterior", List.of()));
            holder.actions.put(45, p -> open(p, page - 1, filter, categoryIndex));
        }
        inv.setItem(46, named(Material.HOPPER, "§eFiltro de tipo: " + filter.label,
                List.of("§7Clique para alternar:", "§fTodos → Máquinas → Itens")));
        holder.actions.put(46, p -> open(p, 0, filter.next(), categoryIndex));

        inv.setItem(47, named(Material.ARROW, "§eCategoria anterior", List.of(
                "§7Atual: §f" + categoryName
        )));
        holder.actions.put(47, p -> open(p, 0, filter,
                categoryIndex == 0 ? categories.size() : categoryIndex - 1));

        inv.setItem(48, named(Material.BOOK, "§bCategoria: " + categoryName, List.of(
                "§7Clique para avançar para a próxima categoria.",
                "§7Categorias disponíveis: §f" + categories.size()
        )));
        holder.actions.put(48, p -> open(p, 0, filter,
                categoryIndex >= categories.size() ? 0 : categoryIndex + 1));

        inv.setItem(49, named(Material.BARRIER, "§cFechar", List.of()));
        holder.actions.put(49, Player::closeInventory);

        inv.setItem(50, named(Material.NETHER_STAR, "§bSeus pontos: "
                + api.getResearchPoints(player.getUniqueId()), List.of(
                "§7Ganhe pontos minerando novos minérios,",
                "§7explorando novos biomas e concluindo conquistas.",
                "§7Use /mechanium points para consultar."
        )));
        holder.actions.put(50, p -> {
            p.sendMessage("§bPontos de pesquisa disponíveis: §f" + api.getResearchPoints(p.getUniqueId()));
            p.sendMessage("§7Minere novos tipos de minério, explore biomas inéditos e conclua conquistas.");
        });

        inv.setItem(51, named(Material.CRAFTING_TABLE, "§aComo fabricar", List.of(
                "§71. Escolha a categoria e o tipo.",
                "§72. Passe o mouse para ver os ingredientes.",
                "§73. Clique no resultado para fabricar.",
                "§7Os itens necessários serão consumidos."
        )));
        if (categoryIndex != 0 || filter != ItemFilter.ALL) {
            inv.setItem(52, named(Material.MILK_BUCKET, "§eLimpar filtros", List.of("§7Mostrar todos os itens e máquinas.")));
            holder.actions.put(52, p -> open(p, 0, ItemFilter.ALL, 0));
        }
        if (page + 1 < pages) {
            inv.setItem(53, named(Material.ARROW, "§ePróxima página", List.of()));
            holder.actions.put(53, p -> open(p, page + 1, filter, categoryIndex));
        }
        player.openInventory(inv);
    }

    private void craft(Player player, RecipeDefinition recipe) {
        ItemDefinition resultDefinition = api.getItem(recipe.resultItemId()).orElse(null);
        if (resultDefinition == null) return;
        if (!api.canCraft(player.getUniqueId(), recipe.id())
                || !api.canUseItem(player.getUniqueId(), resultDefinition.id())) {
            player.sendMessage("§cTecnologia bloqueada. Abra a árvore de pesquisas para verificar os pré-requisitos.");
            return;
        }

        Map<String, Integer> required = ingredientCounts(recipe);
        ItemStack[] simulatedContents = cloneContents(player.getInventory().getStorageContents());
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            int available = count(simulatedContents, entry.getKey());
            if (available < entry.getValue()) {
                player.sendMessage("§cFaltam ingredientes: §f" + ingredientLabel(entry.getKey())
                        + " x" + (entry.getValue() - available) + "§c.");
                return;
            }
            consume(simulatedContents, entry.getKey(), entry.getValue());
        }

        ItemStack result = api.createItemStack(recipe.resultItemId(), recipe.resultAmount()).orElse(null);
        if (result == null) {
            player.sendMessage("§cO resultado desta receita não pôde ser criado.");
            return;
        }

        Inventory simulation = Bukkit.createInventory(null, 36);
        simulation.setStorageContents(simulatedContents);
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            Material remainder = craftingRemainder(entry.getKey());
            if (remainder != null) {
                Map<Integer, ItemStack> leftovers = simulation.addItem(new ItemStack(remainder, entry.getValue()));
                if (!leftovers.isEmpty()) {
                    player.sendMessage("§cLibere espaço no inventário para receber o recipiente vazio e o resultado.");
                    return;
                }
            }
        }
        if (!simulation.addItem(result).isEmpty()) {
            player.sendMessage("§cLibere espaço no inventário antes de fabricar.");
            return;
        }

        player.getInventory().setStorageContents(simulation.getStorageContents());
        player.sendMessage("§aFabricado: §f" + resultDefinition.displayName()
                + " §7x" + recipe.resultAmount() + "§a. Ingredientes consumidos.");
    }

    private boolean hasIngredients(Player player, RecipeDefinition recipe) {
        return ingredientCounts(recipe).entrySet().stream()
                .allMatch(entry -> count(player.getInventory().getStorageContents(), entry.getKey()) >= entry.getValue());
    }

    private Map<String, Integer> ingredientCounts(RecipeDefinition recipe) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (recipe.type() == RecipeDefinition.Type.SHAPELESS) {
            recipe.ingredients().values().forEach(value -> counts.merge(value, 1, Integer::sum));
        } else {
            recipe.shape().forEach(row -> row.chars().filter(character -> character != ' ').forEach(character ->
                    counts.merge(recipe.ingredients().get((char) character), 1, Integer::sum)));
        }
        return counts;
    }

    private int count(Player player, String ingredient) {
        return count(player.getInventory().getStorageContents(), ingredient);
    }

    private int count(ItemStack[] contents, String ingredient) {
        int total = 0;
        for (ItemStack stack : contents) {
            if (matches(stack, ingredient)) total += stack.getAmount();
        }
        return total;
    }

    private void consume(ItemStack[] contents, String ingredient, int amount) {
        int remaining = amount;
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (!matches(stack, ingredient)) continue;
            int take = Math.min(remaining, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) contents[i] = null;
            remaining -= take;
        }
    }

    private ItemStack[] cloneContents(ItemStack[] source) {
        ItemStack[] clone = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) clone[i] = source[i] == null ? null : source[i].clone();
        return clone;
    }

    private Material craftingRemainder(String ingredient) {
        return switch (ingredient) {
            case "WATER_BUCKET", "LAVA_BUCKET", "MILK_BUCKET", "POWDER_SNOW_BUCKET" -> Material.BUCKET;
            default -> null;
        };
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
        meta.setDisplayName(name.replace('&', '§'));
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

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) event.setCancelled(true);
    }

    private enum ItemFilter {
        ALL("Todos"), MACHINES("Máquinas"), ITEMS("Itens / componentes");

        private final String label;
        ItemFilter(String label) { this.label = label; }
        private ItemFilter next() {
            return switch (this) {
                case ALL -> MACHINES;
                case MACHINES -> ITEMS;
                case ITEMS -> ALL;
            };
        }
    }

    private static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
        @Override public Inventory getInventory() { return inventory; }
    }
}
