package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.MachineDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.Consumer;

/** Read-only encyclopedia for MECHANIUM items, recipes and machines. */
final class MechaniumWikiMenu implements Listener {
    private static final int SIZE = 54;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
    };

    private final MechaniumApi api;

    MechaniumWikiMenu(MechaniumPlugin plugin, MechaniumApi api) {
        this.api = api;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    void openHome(Player player) { openHome(player, Player::closeInventory); }

    void openHome(Player player, Consumer<Player> backAction) {
        Holder holder = create("MECHANIUM • Wiki");
        fillFrame(holder.inventory);
        holder.inventory.setItem(4, item(Material.BOOK, "§6§lWIKI DO MECHANIUM", List.of(
                "§7Enciclopédia de consulta do conteúdo.",
                "§7Nenhum ingrediente é consumido nesta área.",
                "§7Itens: §f" + api.getItems().size(),
                "§7Receitas: §f" + api.getRecipes().size(),
                "§7Máquinas: §f" + api.getMachines().size()
        )));
        button(holder, 20, Material.CHEST, "§eItens e materiais",
                List.of("§7Consulte materiais, categorias e pesquisas."), p -> openItems(p, 0));
        button(holder, 22, Material.WRITABLE_BOOK, "§bReceitas",
                List.of("§7Veja ingredientes, resultados e requisitos."), p -> openRecipes(p, 0));
        button(holder, 24, Material.BLAST_FURNACE, "§6Máquinas",
                List.of("§7Veja entradas, saídas e tempo de processamento."), p -> openMachines(p, 0));
        button(holder, 49, Material.ARROW, "§eVoltar ao menu principal", List.of(), backAction);
        open(player, holder);
    }

    private void openItems(Player player, int requestedPage) {
        List<ItemDefinition> entries = api.getItems().stream()
                .sorted(Comparator.comparing(ItemDefinition::categoryId)
                        .thenComparing(ItemDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int page = page(requestedPage, entries.size());
        Holder holder = listHolder("MECHANIUM • Wiki • Itens", "§eITENS E MATERIAIS",
                entries.size(), page, "materiais registrados");
        int start = page * SLOTS.length;
        for (int i = 0; i < Math.min(SLOTS.length, entries.size() - start); i++) {
            ItemDefinition entry = entries.get(start + i);
            Material material = icon(entry.material());
            int slot = SLOTS[i];
            holder.inventory.setItem(slot, item(material, "§e" + entry.displayName(), List.of(
                    "§7Categoria: §f" + categoryName(entry.categoryId()),
                    "§7Identificador: §8" + entry.id(),
                    "§7Pesquisa: §f" + Objects.toString(entry.requiredTechnologyId(), "Nenhuma"),
                    "§aClique para ver detalhes."
            )));
            holder.actions.put(slot, p -> openItemDetail(p, entry, page));
        }
        finishList(player, holder, "itens", page, entries.size());
    }

    private void openRecipes(Player player, int requestedPage) {
        List<RecipeDefinition> entries = api.getRecipes().stream()
                .sorted(Comparator.comparing((RecipeDefinition recipe) -> itemName(recipe.resultItemId()),
                        String.CASE_INSENSITIVE_ORDER).thenComparing(RecipeDefinition::id))
                .toList();
        int page = page(requestedPage, entries.size());
        Holder holder = listHolder("MECHANIUM • Wiki • Receitas", "§bRECEITAS", entries.size(), page,
                "receitas registradas");
        int start = page * SLOTS.length;
        for (int i = 0; i < Math.min(SLOTS.length, entries.size() - start); i++) {
            RecipeDefinition entry = entries.get(start + i);
            ItemDefinition result = api.getItem(entry.resultItemId()).orElse(null);
            int slot = SLOTS[i];
            holder.inventory.setItem(slot, item(result == null ? Material.PAPER : icon(result.material()),
                    "§b" + itemName(entry.resultItemId()), List.of(
                            "§7Produção: §fx" + entry.resultAmount(),
                            "§7Tipo: §f" + recipeType(entry),
                            "§7Pesquisa: §f" + Objects.toString(entry.requiredTechnologyId(), "Nenhuma"),
                            "§aClique para ver a receita completa."
                    )));
            holder.actions.put(slot, p -> openRecipeDetail(p, entry, page));
        }
        finishList(player, holder, "receitas", page, entries.size());
    }

    private void openMachines(Player player, int requestedPage) {
        List<MachineDefinition> entries = api.getMachines().stream()
                .sorted(Comparator.comparing(MachineDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int page = page(requestedPage, entries.size());
        Holder holder = listHolder("MECHANIUM • Wiki • Máquinas", "§6MÁQUINAS", entries.size(), page,
                "máquinas registradas");
        int start = page * SLOTS.length;
        for (int i = 0; i < Math.min(SLOTS.length, entries.size() - start); i++) {
            MachineDefinition entry = entries.get(start + i);
            ItemDefinition machineItem = api.getItem(entry.machineItemId()).orElse(null);
            int slot = SLOTS[i];
            holder.inventory.setItem(slot, item(machineItem == null ? Material.DISPENSER : icon(machineItem.material()),
                    "§6" + entry.displayName(), List.of(
                            "§7Entradas: §f" + machineInputsSummary(entry),
                            "§7Saídas: §f" + machineOutputsSummary(entry),
                            "§7Ciclo: §f" + formatTicks(entry.processingTicks()),
                            "§7Energia/operação: §f" + energyCostLabel(entry.energyCostPerOperation()),
                            "§aClique para ver detalhes."
                    )));
            holder.actions.put(slot, p -> openMachineDetail(p, entry, page));
        }
        finishList(player, holder, "máquinas", page, entries.size());
    }

    private Holder listHolder(String title, String heading, int total, int page, String description) {
        Holder holder = create(title);
        fillFrame(holder.inventory);
        holder.inventory.setItem(4, item(Material.BOOK, heading, List.of(
                "§7Total: §f" + total + " " + description,
                "§7Página: §f" + (page + 1) + "/" + pages(total),
                "§8Clique em uma entrada para consultar."
        )));
        if (total == 0) holder.inventory.setItem(22,
                item(Material.BARRIER, "§cNenhum conteúdo registrado", List.of("§7Esta seção está vazia.")));
        return holder;
    }

    private void finishList(Player player, Holder holder, String section, int page, int total) {
        if (page > 0) button(holder, 45, Material.ARROW, "§ePágina anterior", List.of(),
                p -> openSection(p, section, page - 1));
        if (page + 1 < pages(total)) button(holder, 53, Material.ARROW, "§ePróxima página", List.of(),
                p -> openSection(p, section, page + 1));
        button(holder, 49, Material.BARRIER, "§cVoltar à Wiki", List.of(), this::openHome);
        open(player, holder);
    }

    private void openSection(Player player, String section, int page) {
        switch (section) {
            case "itens" -> openItems(player, page);
            case "receitas" -> openRecipes(player, page);
            case "máquinas" -> openMachines(player, page);
            default -> openHome(player);
        }
    }

    private void openItemDetail(Player player, ItemDefinition entry, int page) {
        Holder holder = create("MECHANIUM • Wiki • " + entry.displayName());
        fillFrame(holder.inventory);
        holder.inventory.setItem(13, item(icon(entry.material()), "§e" + entry.displayName(), List.of(
                "§7ID: §8" + entry.id(),
                "§7Material visual: §f" + entry.material(),
                "§7Limite por pilha: §f" + entry.maxStackSize()
        )));
        List<String> lore = new ArrayList<>();
        lore.add("§7Categoria: §f" + categoryName(entry.categoryId()));
        lore.add("§7Pesquisa necessária: §f" + Objects.toString(entry.requiredTechnologyId(), "Nenhuma"));
        lore.add("§7Modelo customizado: §f" + entry.customModelData());
        lore.add("§7Descrição:");
        if (entry.lore().isEmpty()) lore.add(" §8Sem descrição adicional.");
        else entry.lore().forEach(line -> lore.add(" §f" + line));
        long recipeCount = api.getRecipes().stream().filter(r -> r.resultItemId().equals(entry.id())).count();
        lore.add("§7Receitas que produzem este item: §f" + recipeCount);
        long machineCount = api.getMachines().stream().filter(m -> m.machineItemId().equals(entry.id())).count();
        lore.add("§7Máquinas associadas: §f" + machineCount);
        holder.inventory.setItem(22, item(Material.PAPER, "§fInformações do item", lore));
        button(holder, 45, Material.ARROW, "§eVoltar aos itens", List.of(), p -> openItems(p, page));
        button(holder, 49, Material.BARRIER, "§cFechar", List.of(), Player::closeInventory);
        open(player, holder);
    }

    private void openRecipeDetail(Player player, RecipeDefinition entry, int page) {
        Holder holder = create("MECHANIUM • Wiki • Receita");
        fillFrame(holder.inventory);
        ItemDefinition result = api.getItem(entry.resultItemId()).orElse(null);
        holder.inventory.setItem(13, item(result == null ? Material.PAPER : icon(result.material()),
                "§b" + itemName(entry.resultItemId()), List.of("§7Resultado: §fx" + entry.resultAmount())));
        List<String> lore = new ArrayList<>();
        lore.add("§7ID da receita: §8" + entry.id());
        lore.add("§7Formato: §f" + recipeType(entry));
        lore.add("§7Pesquisa necessária: §f" + Objects.toString(entry.requiredTechnologyId(), "Nenhuma"));
        lore.add("§7Ingredientes:");
        ingredientCounts(entry).forEach((ingredient, amount) ->
                lore.add(" §8• §f" + ingredientLabel(ingredient) + " x" + amount));
        if (entry.type() == RecipeDefinition.Type.SHAPED) {
            lore.add("§7Formato da bancada:");
            entry.shape().forEach(row -> lore.add(" §f[" + row.replace(' ', '·') + "]"));
        }
        holder.inventory.setItem(22, item(Material.WRITABLE_BOOK, "§fDetalhes da receita", lore));
        button(holder, 45, Material.ARROW, "§eVoltar às receitas", List.of(), p -> openRecipes(p, page));
        button(holder, 49, Material.BARRIER, "§cFechar", List.of(), Player::closeInventory);
        open(player, holder);
    }

    private void openMachineDetail(Player player, MachineDefinition entry, int page) {
        Holder holder = create("MECHANIUM • Wiki • Máquina");
        fillFrame(holder.inventory);
        ItemDefinition machineItem = api.getItem(entry.machineItemId()).orElse(null);
        holder.inventory.setItem(13, item(machineItem == null ? Material.DISPENSER : icon(machineItem.material()),
                "§6" + entry.displayName(), List.of("§7Item de máquina: §f" + entry.machineItemId())));

        List<String> details = new ArrayList<>();
        details.add("§7Identificador: §8" + entry.id());
        details.add("§7Bloco-base: §f" + entry.blockMaterial());
        details.add("§bEntradas:");
        entry.inputs().forEach(input -> details.add("§7• §f" + ingredientLabel(input.itemId())
                + " x" + input.amount()));
        details.add("§aProdutos:");
        entry.outputs().forEach(output -> details.add("§7• §f" + itemName(output.itemId())
                + " x" + output.amount()));
        details.add("§7Tempo por ciclo: §f" + formatTicks(entry.processingTicks()));
        details.add("§7Energia por operação: §f" + energyCostLabel(entry.energyCostPerOperation()));
        details.add("§7Pesquisa necessária: §f" + entry.requiredTechnologyId());
        details.add("§8Slots 1–6 recebem os ingredientes; 7–9 armazenam os produtos.");

        holder.inventory.setItem(22, item(Material.BOOK, "§fFicha técnica", details));
        button(holder, 45, Material.ARROW, "§eVoltar às máquinas", List.of(), p -> openMachines(p, page));
        button(holder, 49, Material.BARRIER, "§cFechar", List.of(), Player::closeInventory);
        open(player, holder);
    }

    private Map<String, Integer> ingredientCounts(RecipeDefinition recipe) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (recipe.type() == RecipeDefinition.Type.SHAPELESS) {
            recipe.ingredients().values().forEach(value -> counts.merge(value, 1, Integer::sum));
        } else {
            recipe.shape().forEach(row -> row.chars().filter(c -> c != ' ')
                    .forEach(c -> counts.merge(recipe.ingredients().get((char) c), 1, Integer::sum)));
        }
        return counts;
    }

    private String machineInputsSummary(MachineDefinition machine) {
        String summary = machine.inputs().stream().limit(2)
                .map(input -> ingredientLabel(input.itemId()) + " x" + input.amount())
                .collect(java.util.stream.Collectors.joining(", "));
        return machine.inputs().size() > 2 ? summary + ", +" + (machine.inputs().size() - 2) : summary;
    }

    private String machineOutputsSummary(MachineDefinition machine) {
        String summary = machine.outputs().stream().limit(2)
                .map(output -> itemName(output.itemId()) + " x" + output.amount())
                .collect(java.util.stream.Collectors.joining(", "));
        return machine.outputs().size() > 2 ? summary + ", +" + (machine.outputs().size() - 2) : summary;
    }

    private String energyCostLabel(int energyCost) {
        return energyCost == 0 ? "Não exige energia" : energyCost + " unidades";
    }

    private String ingredientLabel(String ingredient) {
        if (ingredient.regionMatches(true, 0, "mechanium:", 0, 10)) {
            return itemName(ingredient.substring(10));
        }
        return ingredient.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String itemName(String id) {
        return api.getItem(id).map(ItemDefinition::displayName).orElse(id);
    }

    private String categoryName(String id) {
        return api.getCategory(id).map(category -> category.displayName()).orElse(id);
    }

    private String recipeType(RecipeDefinition recipe) {
        return recipe.type() == RecipeDefinition.Type.SHAPED ? "Modelada" : "Sem formato";
    }

    private String formatTicks(int ticks) {
        double seconds = ticks / 20.0d;
        return (seconds == Math.floor(seconds) ? Long.toString((long) seconds) : String.format(Locale.ROOT, "%.1f", seconds))
                + " segundos";
    }

    private int pages(int total) {
        return Math.max(1, (total + SLOTS.length - 1) / SLOTS.length);
    }

    private int page(int requested, int total) {
        return Math.max(0, Math.min(requested, pages(total) - 1));
    }

    private Material icon(String materialName) {
        Material material = Material.matchMaterial(materialName);
        return material != null && material.isItem() ? material : Material.BARRIER;
    }

    private Holder create(String title) {
        Holder holder = new Holder();
        holder.inventory = Bukkit.createInventory(holder, SIZE, Component.text(title));
        return holder;
    }

    private void open(Player player, Holder holder) {
        holder.animating = false;
        player.openInventory(holder.inventory);
    }

    private void fillFrame(Inventory inventory) {
        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) inventory.setItem(slot, pane);
        }
    }

    private void button(Holder holder, int slot, Material material, String name, List<String> lore,
                        Consumer<Player> action) {
        holder.inventory.setItem(slot, item(material, name, lore));
        holder.actions.put(slot, action);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
        if (!lore.isEmpty()) meta.lore(lore.stream()
                .map(LegacyComponentSerializer.legacySection()::deserialize).toList());
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

    private static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private boolean animating;
        private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
        @Override public Inventory getInventory() { return inventory; }
    }
}
