package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Consumer;

/**
 * Permission-protected editor for registered MECHANIUM recipes.
 * Overrides are persisted by MechaniumPlugin outside module-owned recipe records.
 */
final class MechaniumRecipeAdminMenu implements Listener {
    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
    };

    private final MechaniumPlugin plugin;
    private final JavaPlugin javaPlugin;
    private final MechaniumApi api;
    private final Map<UUID, Draft> drafts = new HashMap<>();
    private final Map<UUID, InputPrompt> prompts = new HashMap<>();

    MechaniumRecipeAdminMenu(MechaniumPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.javaPlugin = plugin;
        this.api = api;
    }

    void open(Player player) {
        if (!plugin.hasAdminAccess(player)) {
            player.sendMessage("§cVocê não tem permissão para editar receitas.");
            return;
        }
        drafts.remove(player.getUniqueId());
        prompts.remove(player.getUniqueId());
        openList(player, 0);
    }

    private void openList(Player player, int requestedPage) {
        List<RecipeDefinition> recipes = api.getRecipes().stream()
                .sorted(Comparator.comparing(RecipeDefinition::id)).toList();
        int pageCount = Math.max(1, (recipes.size() + CONTENT_SLOTS.length - 1) / CONTENT_SLOTS.length);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        MenuHolder holder = create("MECHANIUM • Receitas próprias");
        Inventory inventory = holder.getInventory();
        fillFrame(inventory);
        inventory.setItem(4, item(Material.BOOK, "§6§lEditor de receitas MECHANIUM", List.of(
                "§7Receitas registradas: §f" + recipes.size(),
                "§7Página: §f" + (page + 1) + "§7/§f" + pageCount,
                "§8Edite resultado, quantidade, ingredientes e tecnologia."
        )));
        if (recipes.isEmpty()) {
            inventory.setItem(22, item(Material.BARRIER, "§cNenhuma receita registrada", List.of(
                    "§7Ative os módulos que adicionam receitas."
            )));
        }

        int start = page * CONTENT_SLOTS.length;
        int count = Math.min(CONTENT_SLOTS.length, recipes.size() - start);
        for (int i = 0; i < count; i++) {
            RecipeDefinition recipe = recipes.get(start + i);
            int slot = CONTENT_SLOTS[i];
            ItemStack icon = api.createItemStack(recipe.resultItemId(), 1)
                    .orElseGet(() -> new ItemStack(Material.BARRIER));
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(LegacyComponentSerializer.legacySection()
                        .deserialize("§e" + recipe.id()));
                meta.lore(List.of(
                        Component.text("§7Resultado: §f" + recipe.resultItemId() + " x" + recipe.resultAmount()),
                        Component.text("§7Tipo: §f" + recipe.type()),
                        Component.text("§7Ingredientes: §f" + recipe.ingredients().size()),
                        Component.text("§7Tecnologia: §f" + Objects.toString(recipe.requiredTechnologyId(), "Nenhuma")),
                        Component.text("§aClique para editar.")
                ));
                icon.setItemMeta(meta);
            }
            inventory.setItem(slot, icon);
            holder.actions.put(slot, p -> {
                drafts.put(p.getUniqueId(), Draft.from(recipe));
                openEditor(p);
            });
        }

        if (page > 0) button(holder, 45, Material.ARROW, "§ePágina anterior", List.of(),
                p -> openList(p, page - 1));
        button(holder, 49, Material.BARRIER, "§cVoltar ao painel administrativo", List.of(),
                p -> {
                    drafts.remove(p.getUniqueId());
                    p.performCommand("mechanium admin hud");
                });
        if (page + 1 < pageCount) button(holder, 53, Material.ARROW, "§ePróxima página", List.of(),
                p -> openList(p, page + 1));
        player.openInventory(inventory);
    }

    private void openEditor(Player player) {
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) {
            openList(player, 0);
            return;
        }
        MenuHolder holder = create("MECHANIUM • Editar receita");
        Inventory inventory = holder.getInventory();
        fillFrame(inventory);
        inventory.setItem(4, item(Material.WRITABLE_BOOK, "§6§l" + draft.id, List.of(
                "§7Tipo: §f" + draft.type,
                "§7Resultado: §f" + draft.resultItemId + " x" + draft.resultAmount,
                "§7Tecnologia: §f" + Objects.toString(draft.requiredTechnologyId, "Nenhuma"),
                "§8As alterações só são aplicadas ao clicar em Salvar."
        )));

        ItemStack result = api.createItemStack(draft.resultItemId, Math.min(1,
                api.getItem(draft.resultItemId).map(ItemDefinition::maxStackSize).orElse(1)))
                .orElseGet(() -> new ItemStack(Material.BARRIER));
        setDisplay(result, "§eAlterar resultado", List.of(
                "§7Item MECHANIUM: §f" + draft.resultItemId,
                "§7Clique e digite o ID de outro item registrado."
        ));
        inventory.setItem(10, result);
        holder.actions.put(10, p -> requestInput(p, InputPrompt.RESULT_ITEM,
                "§eDigite no chat o ID do item MECHANIUM que será o resultado. Exemplo: mechanical_parts. Digite cancel para voltar."));

        button(holder, 12, Material.CLOCK, "§bQuantidade: " + draft.resultAmount,
                List.of("§7Clique esquerdo: aumentar.", "§7Clique direito: diminuir.",
                        "§7Limite do item: " + itemStackLimit(draft.resultItemId)),
                p -> changeAmount(p, false));
        holder.rightClickActions.put(12, p -> changeAmount(p, true));

        button(holder, 14, Material.KNOWLEDGE_BOOK, "§dTecnologia exigida",
                List.of("§7Atual: §f" + Objects.toString(draft.requiredTechnologyId, "Nenhuma"),
                        "§7Clique para escolher uma tecnologia."),
                p -> openTechnologySelector(p, 0));
        button(holder, 16, draft.type == RecipeDefinition.Type.SHAPED
                        ? Material.CRAFTING_TABLE : Material.CRAFTING,
                "§aTipo: " + draft.type,
                List.of("§7Clique para alternar SHAPED / SHAPELESS.",
                        "§7Ao mudar para SHAPED, um padrão inicial é criado."),
                this::toggleType);

        int ingredientIndex = 0;
        for (Map.Entry<Character, String> entry : new TreeMap<>(draft.ingredients).entrySet()) {
            if (ingredientIndex >= 9) break;
            int slot = 19 + ingredientIndex;
            String value = entry.getValue();
            Material iconMaterial = ingredientIcon(value);
            button(holder, slot, iconMaterial, "§eIngrediente " + entry.getKey(),
                    List.of("§7Atual: §f" + value,
                            "§7Clique para editar o mapa completo de ingredientes."),
                    p -> requestInput(p, InputPrompt.INGREDIENTS,
                            "§eDigite o mapa completo: A=IRON_INGOT,B=mechanium:copper_coil. Use vírgulas entre os pares. Digite cancel para voltar."));
            ingredientIndex++;
        }
        for (int i = ingredientIndex; i < 9; i++) {
            inventory.setItem(19 + i, item(Material.GRAY_STAINED_GLASS_PANE, "§8Sem ingrediente " + (i + 1),
                    List.of("§7Edite o mapa de ingredientes para adicionar entradas.")));
        }

        if (draft.type == RecipeDefinition.Type.SHAPED) {
            button(holder, 31, Material.MAP, "§bEditar padrão 3×3",
                    List.of("§7Atual: §f" + String.join("/", draft.shape),
                            "§7Use / para separar linhas e . para espaços vazios.",
                            "§7Exemplo: III/IPI/III"),
                    p -> requestInput(p, InputPrompt.SHAPE,
                            "§eDigite o padrão: linhas separadas por / e . para espaços vazios. Exemplo: III/IPI/III. Digite cancel para voltar."));
        } else {
            inventory.setItem(31, item(Material.MAP, "§8Padrão não usado", List.of(
                    "§7Receitas SHAPELESS usam apenas a lista de ingredientes."
            )));
        }

        button(holder, 45, Material.ARROW, "§eVoltar à lista", List.of(), p -> openList(p, 0));
        button(holder, 48, Material.LIME_CONCRETE, "§aSalvar alterações", List.of(
                "§7Valida e registra a receita no Bukkit.",
                "§7O override permanece salvo após reiniciar."
        ), this::saveDraft);
        button(holder, 50, Material.RED_CONCRETE, "§cDescartar alterações", List.of(
                "§7Descarta somente as alterações ainda não salvas."
        ), p -> {
            drafts.remove(p.getUniqueId());
            openList(p, 0);
        });
        button(holder, 53, Material.BARRIER, "§cFechar", List.of(), p -> {
            drafts.remove(p.getUniqueId());
            p.closeInventory();
        });
        player.openInventory(inventory);
    }

    private void openTechnologySelector(Player player, int requestedPage) {
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) return;
        List<TechnologyDefinition> technologies = api.getTechnologies().stream()
                .sorted(Comparator.comparingInt((TechnologyDefinition t) -> api.getEra(t.requiredEraId())
                                .map(era -> era.order()).orElse(Integer.MAX_VALUE))
                        .thenComparing(TechnologyDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int pageCount = Math.max(1, (technologies.size() + CONTENT_SLOTS.length - 1) / CONTENT_SLOTS.length);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        MenuHolder holder = create("MECHANIUM • Tecnologia da receita");
        Inventory inventory = holder.getInventory();
        fillFrame(inventory);
        inventory.setItem(4, item(Material.KNOWLEDGE_BOOK, "§dTecnologia exigida", List.of(
                "§7A receita ficará bloqueada até a pesquisa.",
                "§7A era necessária é definida pela tecnologia.",
                "§7Atual: §f" + Objects.toString(draft.requiredTechnologyId, "Nenhuma")
        )));
        button(holder, 45, Material.BARRIER, "§cNenhuma tecnologia", List.of(
                "§7Remove o requisito específico da receita."
        ), p -> {
            draft.requiredTechnologyId = null;
            openEditor(p);
        });
        button(holder, 49, Material.ARROW, "§eVoltar ao editor", List.of(), this::openEditor);
        int start = page * CONTENT_SLOTS.length;
        int count = Math.min(CONTENT_SLOTS.length, technologies.size() - start);
        for (int i = 0; i < count; i++) {
            TechnologyDefinition technology = technologies.get(start + i);
            int slot = CONTENT_SLOTS[i];
            String eraName = api.getEra(technology.requiredEraId())
                    .map(era -> era.displayName()).orElse(technology.requiredEraId());
            button(holder, slot, Material.ENCHANTED_BOOK, "§e" + technology.displayName(),
                    List.of("§7ID: §f" + technology.id(), "§7Era: §f" + eraName,
                            "§7Custo: §b" + technology.researchCost() + " pontos"),
                    p -> {
                        draft.requiredTechnologyId = technology.id();
                        openEditor(p);
                    });
        }
        if (page > 0) button(holder, 46, Material.PAPER, "§ePágina anterior", List.of(),
                p -> openTechnologySelector(p, page - 1));
        if (page + 1 < pageCount) button(holder, 52, Material.PAPER, "§ePróxima página", List.of(),
                p -> openTechnologySelector(p, page + 1));
        player.openInventory(inventory);
    }

    private void requestInput(Player player, InputPrompt prompt, String message) {
        if (!plugin.hasAdminAccess(player)) {
            player.sendMessage("§cVocê não tem permissão para editar receitas.");
            player.closeInventory();
            return;
        }
        prompts.put(player.getUniqueId(), prompt);
        player.closeInventory();
        player.sendMessage(message);
    }

    @EventHandler
    public void onAdminChatInput(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        InputPrompt prompt = prompts.get(playerId);
        if (prompt == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(javaPlugin, () -> handleInput(event.getPlayer(), prompt, message));
    }

    private void handleInput(Player player, InputPrompt prompt, String message) {
        if (!player.isOnline()) {
            prompts.remove(player.getUniqueId());
            drafts.remove(player.getUniqueId());
            return;
        }
        if (!plugin.hasAdminAccess(player)) {
            prompts.remove(player.getUniqueId());
            player.sendMessage("§cSeu acesso administrativo não está mais ativo.");
            return;
        }
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) {
            prompts.remove(player.getUniqueId());
            return;
        }
        String value = message.trim();
        if (value.equalsIgnoreCase("cancel")) {
            prompts.remove(player.getUniqueId());
            player.sendMessage("§7Edição cancelada.");
            openEditor(player);
            return;
        }
        try {
            switch (prompt) {
                case RESULT_ITEM -> {
                    String itemId = value.toLowerCase(Locale.ROOT);
                    if (itemId.startsWith("mechanium:")) itemId = itemId.substring(10);
                    ItemDefinition result = api.getItem(itemId)
                            .orElseThrow(() -> new IllegalArgumentException("ID de item MECHANIUM não encontrado: " + itemId));
                    draft.resultItemId = result.id();
                    draft.resultAmount = Math.min(draft.resultAmount, result.maxStackSize());
                }
                case INGREDIENTS -> draft.ingredients = parseIngredients(value);
                case SHAPE -> draft.shape = parseShape(value);
            }
            prompts.remove(player.getUniqueId());
            player.sendMessage("§aValor atualizado no rascunho. Clique em Salvar alterações para aplicar.");
            openEditor(player);
        } catch (RuntimeException exception) {
            player.sendMessage("§cEntrada inválida: " + exception.getMessage());
            player.sendMessage("§eTente novamente ou digite cancel.");
        }
    }

    private Map<Character, String> parseIngredients(String input) {
        Map<Character, String> parsed = new TreeMap<>();
        for (String token : input.split(",")) {
            String[] pair = token.trim().split("=", 2);
            if (pair.length != 2 || pair[0].trim().length() != 1) {
                throw new IllegalArgumentException("Use pares no formato A=IRON_INGOT.");
            }
            char symbol = pair[0].trim().charAt(0);
            if (symbol == ' ' || symbol == '.') throw new IllegalArgumentException("Símbolos inválidos: use letras ou números.");
            String ingredient = pair[1].trim();
            if (ingredient.regionMatches(true, 0, "mechanium:", 0, 10)) {
                String customId = ingredient.substring(10).toLowerCase(Locale.ROOT);
                api.getItem(customId).orElseThrow(() -> new IllegalArgumentException(
                        "Item MECHANIUM não encontrado: " + customId));
                ingredient = "mechanium:" + customId;
            } else {
                Material material = Material.matchMaterial(ingredient);
                if (material == null || !material.isItem()) {
                    throw new IllegalArgumentException("Material inválido ou não é item: " + ingredient);
                }
                ingredient = material.name();
            }
            if (parsed.putIfAbsent(symbol, ingredient) != null) {
                throw new IllegalArgumentException("Símbolo repetido: " + symbol);
            }
            if (parsed.size() > 9) throw new IllegalArgumentException("Uma receita pode ter no máximo 9 ingredientes.");
        }
        if (parsed.isEmpty()) throw new IllegalArgumentException("Informe ao menos um ingrediente.");
        return parsed;
    }

    private List<String> parseShape(String input) {
        String[] rows = input.split("/", -1);
        if (rows.length < 1 || rows.length > 3) throw new IllegalArgumentException("O padrão deve ter entre 1 e 3 linhas.");
        List<String> shape = new ArrayList<>();
        int width = -1;
        for (String raw : rows) {
            String row = raw.replace('.', ' ');
            if (row.length() < 1 || row.length() > 3 || row.isBlank()) {
                throw new IllegalArgumentException("Cada linha deve ter entre 1 e 3 colunas e ao menos um símbolo.");
            }
            if (width < 0) width = row.length();
            if (row.length() != width) throw new IllegalArgumentException("Todas as linhas devem ter a mesma largura.");
            shape.add(row);
        }
        return List.copyOf(shape);
    }

    private void changeAmount(Player player, boolean decrease) {
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) return;
        int limit = itemStackLimit(draft.resultItemId);
        int next = decrease ? Math.max(1, draft.resultAmount - 1)
                : Math.min(limit, draft.resultAmount + 1);
        draft.resultAmount = next;
        openEditor(player);
    }

    private int itemStackLimit(String itemId) {
        return api.getItem(itemId).map(ItemDefinition::maxStackSize).orElse(1);
    }

    private void toggleType(Player player) {
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) return;
        if (draft.type == RecipeDefinition.Type.SHAPED) {
            draft.type = RecipeDefinition.Type.SHAPELESS;
            draft.shape = List.of();
        } else {
            draft.type = RecipeDefinition.Type.SHAPED;
            draft.shape = createDefaultShape(draft.ingredients.keySet());
        }
        openEditor(player);
    }

    private List<String> createDefaultShape(Collection<Character> symbols) {
        List<Character> ordered = symbols.stream().sorted().toList();
        if (ordered.isEmpty()) throw new IllegalArgumentException("Adicione ingredientes antes de usar SHAPED.");
        int width = Math.min(3, ordered.size());
        List<String> rows = new ArrayList<>();
        for (int start = 0; start < ordered.size(); start += width) {
            StringBuilder row = new StringBuilder();
            for (int i = 0; i < width; i++) {
                int index = start + i;
                row.append(index < ordered.size() ? ordered.get(index) : ' ');
            }
            rows.add(row.toString());
        }
        return List.copyOf(rows);
    }

    private void saveDraft(Player player) {
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) return;
        try {
            RecipeDefinition replacement = new RecipeDefinition(draft.id, draft.type, draft.resultItemId,
                    draft.resultAmount, draft.shape, draft.ingredients, draft.requiredTechnologyId);
            plugin.saveAdminRecipeOverride(replacement);
            player.sendMessage("§aReceita §f" + draft.id + " §aatualizada e salva no config.yml.");
            drafts.put(player.getUniqueId(), Draft.from(replacement));
            openEditor(player);
        } catch (RuntimeException exception) {
            player.sendMessage("§cNão foi possível salvar: " + exception.getMessage());
        }
    }

    private Material ingredientIcon(String value) {
        if (value.startsWith("mechanium:")) {
            return api.getItem(value.substring(10)).map(item -> Material.matchMaterial(item.material()))
                    .orElse(Material.BARRIER);
        }
        Material material = Material.matchMaterial(value);
        return material == null ? Material.BARRIER : material;
    }

    private MenuHolder create(String title) {
        MenuHolder holder = new MenuHolder();
        holder.inventory = Bukkit.createInventory(holder, SIZE, Component.text(title));
        return holder;
    }

    private void fillFrame(Inventory inventory) {
        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) inventory.setItem(slot, pane);
        }
    }

    private void button(MenuHolder holder, int slot, Material material, String name,
                        List<String> lore, Consumer<Player> action) {
        holder.inventory.setItem(slot, item(material, name, lore));
        holder.actions.put(slot, action);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        setDisplay(stack, name, lore);
        return stack;
    }

    private void setDisplay(ItemStack stack, String name, List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(LegacyComponentSerializer.legacySection()::deserialize).toList());
        }
        stack.setItemMeta(meta);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!plugin.hasAdminAccess(player)) {
            player.closeInventory();
            player.sendMessage("§cSeu acesso administrativo não está mais ativo.");
            return;
        }
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        int slot = event.getSlot();
        Consumer<Player> action = event.isRightClick()
                ? holder.rightClickActions.getOrDefault(slot, holder.actions.get(slot))
                : holder.actions.get(slot);
        if (action != null) action.accept(player);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    private enum InputPrompt { RESULT_ITEM, INGREDIENTS, SHAPE }

    private static final class Draft {
        private final String id;
        private RecipeDefinition.Type type;
        private String resultItemId;
        private int resultAmount;
        private List<String> shape;
        private Map<Character, String> ingredients;
        private String requiredTechnologyId;

        private Draft(String id, RecipeDefinition.Type type, String resultItemId, int resultAmount,
                      List<String> shape, Map<Character, String> ingredients, String requiredTechnologyId) {
            this.id = id;
            this.type = type;
            this.resultItemId = resultItemId;
            this.resultAmount = resultAmount;
            this.shape = new ArrayList<>(shape);
            this.ingredients = new TreeMap<>(ingredients);
            this.requiredTechnologyId = requiredTechnologyId;
        }

        private static Draft from(RecipeDefinition recipe) {
            return new Draft(recipe.id(), recipe.type(), recipe.resultItemId(), recipe.resultAmount(),
                    recipe.shape(), recipe.ingredients(), recipe.requiredTechnologyId());
        }
    }

    private static final class MenuHolder implements InventoryHolder {
        private Inventory inventory;
        private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
        private final Map<Integer, Consumer<Player>> rightClickActions = new HashMap<>();
        @Override public Inventory getInventory() { return inventory; }
    }
}
