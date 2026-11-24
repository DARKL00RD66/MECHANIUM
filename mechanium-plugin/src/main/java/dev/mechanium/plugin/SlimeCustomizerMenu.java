package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.MechaniumContentRegistrationScope;
import dev.mechanium.api.model.ItemDefinition;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Initial administrative item editor for Slime Customizer.
 *
 * User-created definitions have a dedicated ownership scope per item. The API's
 * SQLite ownership registry can therefore clean up one custom item without
 * deleting the built-in module's templates or other administrator-created items.
 */
final class SlimeCustomizerMenu implements Listener {
    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
    };
    private static final String USER_ITEM_IDS_PATH = "slime-customizer.user-item-ids";
    private static final String CATEGORY_ID = "customizer_user_items";

    private final MechaniumPlugin plugin;
    private final JavaPlugin javaPlugin;
    private final MechaniumApi api;
    private final Map<UUID, Draft> drafts = new HashMap<>();
    private final Map<UUID, ItemDefinition> editingItems = new HashMap<>();
    private final Map<UUID, ChatPrompt> prompts = new ConcurrentHashMap<>();

    SlimeCustomizerMenu(MechaniumPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.javaPlugin = plugin;
        this.api = api;
    }

    void open(Player player) {
        if (!plugin.hasAdminAccess(player)) {
            player.sendMessage("§cVocê não tem permissão para usar o Slime Customizer.");
            return;
        }
        if (!api.getRegisteredModules().contains("slime-customizer")) {
            player.sendMessage("§cAtive o módulo slime-customizer em config.yml para usar este editor.");
            return;
        }
        drafts.remove(player.getUniqueId());
        prompts.remove(player.getUniqueId());
        openList(player, 0);
    }

    private void openList(Player player, int requestedPage) {
        List<ItemDefinition> items = configuredItems();
        int pageCount = Math.max(1, (items.size() + CONTENT_SLOTS.length - 1) / CONTENT_SLOTS.length);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        MenuHolder holder = create("MECHANIUM • Slime Customizer");
        Inventory inventory = holder.getInventory();
        fillFrame(inventory);
        inventory.setItem(4, item(Material.WRITABLE_BOOK, "§6§lOficina de personalização", List.of(
                "§7Itens personalizados: §f" + items.size(),
                "§7Página: §f" + (page + 1) + "§7/§f" + pageCount,
                "§8Crie itens usando materiais, categorias",
                "§8e pesquisas que já existem no MECHANIUM."
        )));

        int start = page * CONTENT_SLOTS.length;
        int count = Math.min(CONTENT_SLOTS.length, items.size() - start);
        for (int i = 0; i < count; i++) {
            ItemDefinition definition = items.get(start + i);
            Material material = Material.matchMaterial(definition.material());
            ItemStack icon = new ItemStack(material == null ? Material.BARRIER : material);
            setDisplay(icon, "§e" + definition.displayName(), List.of(
                    "§7ID: §f" + definition.id(),
                    "§7Material: §f" + definition.material(),
                    "§7Categoria: §f" + definition.categoryId(),
                    "§7Pesquisa: §f" + Objects.toString(definition.requiredTechnologyId(), "Nenhuma"),
                    "§7Pilha máxima: §f" + definition.maxStackSize(),
                    "§aClique para ver opções."
            ));
            int slot = CONTENT_SLOTS[i];
            inventory.setItem(slot, icon);
            holder.actions.put(slot, p -> openItem(p, definition, page));
        }

        if (page > 0) button(holder, 45, Material.ARROW, "§ePágina anterior", List.of(),
                p -> openList(p, page - 1));
        button(holder, 47, Material.ANVIL, "§aCriar item", List.of(
                "§7Abre o assistente de criação.",
                "§7Use um ID novo e escolha um material válido."
        ), this::beginCreate);
        button(holder, 49, Material.ARROW, "§eVoltar ao painel administrativo", List.of(),
                p -> p.performCommand("mechanium admin hud"));
        if (page + 1 < pageCount) button(holder, 53, Material.ARROW, "§ePróxima página", List.of(),
                p -> openList(p, page + 1));
        open(player, holder);
    }

    private void openItem(Player player, ItemDefinition definition, int page) {
        MenuHolder holder = create("MECHANIUM • Item personalizado");
        fillFrame(holder.getInventory());
        Material material = Material.matchMaterial(definition.material());
        holder.getInventory().setItem(13, item(material == null ? Material.BARRIER : material,
                "§e" + definition.displayName(), List.of(
                        "§7ID: §f" + definition.id(),
                        "§7Material: §f" + definition.material(),
                        "§7Categoria: §f" + definition.categoryId(),
                        "§7Pesquisa: §f" + Objects.toString(definition.requiredTechnologyId(), "Nenhuma"),
                        "§7Pilha máxima: §f" + definition.maxStackSize(),
                        "§8Este item é persistido com ownership próprio."
                )));
        button(holder, 20, Material.NAME_TAG, "§eEditar nome", List.of(
                "§7Altera somente o nome visível do item."
        ), p -> beginEdit(p, definition, ChatPrompt.EDIT_DISPLAY_NAME,
                "§eDigite o novo nome visível. Cores com & são aceitas:"));
        button(holder, 21, Material.ANVIL, "§eEditar material", List.of(
                "§7Troca o material base, se não invalidar",
                "§7máquinas, receitas ou drops existentes."
        ), p -> beginEdit(p, definition, ChatPrompt.EDIT_MATERIAL,
                "§eDigite o novo material Minecraft (ex.: COPPER_INGOT):"));
        button(holder, 22, Material.KNOWLEDGE_BOOK, "§eEditar pesquisa", List.of(
                "§7Use um ID de pesquisa existente ou none."
        ), p -> beginEdit(p, definition, ChatPrompt.EDIT_TECHNOLOGY,
                "§eDigite o ID de uma pesquisa existente ou none:"));
        button(holder, 23, Material.CHEST, "§eEditar pilha máxima", List.of(
                "§7Limite de 1 a 64. A alteração é rejeitada",
                "§7se quebrar uma receita ou máquina existente."
        ), p -> beginEdit(p, definition, ChatPrompt.EDIT_MAX_STACK,
                "§eDigite a nova pilha máxima (1–64):"));
        button(holder, 29, Material.CHEST, "§aEntregar uma unidade", List.of(
                "§7Adiciona uma unidade ao seu inventário.",
                "§7As regras de pesquisa continuam ativas."
        ), p -> giveItem(p, definition, page));
        button(holder, 31, Material.BARRIER, "§cRemover item", List.of(
                "§7Remove somente este item personalizado.",
                "§7Itens e conteúdo de outros módulos não são afetados."
        ), p -> confirmDelete(p, definition, page));
        button(holder, 49, Material.ARROW, "§eVoltar à lista", List.of(), p -> openList(p, page));
        open(player, holder);
    }

    private void confirmDelete(Player player, ItemDefinition definition, int page) {
        MenuHolder holder = create("MECHANIUM • Confirmar remoção");
        fillFrame(holder.getInventory());
        holder.getInventory().setItem(22, item(Material.BARRIER, "§cRemover " + definition.id() + "?", List.of(
                "§7Apenas o item personalizado será removido.",
                "§7A operação não pode ser desfeita."
        )));
        button(holder, 29, Material.LIME_CONCRETE, "§aConfirmar remoção", List.of(), p -> {
            if (deleteItem(definition.id(), p)) openList(p, page);
        });
        button(holder, 33, Material.RED_CONCRETE, "§cCancelar", List.of(),
                p -> openItem(p, definition, page));
        open(player, holder);
    }

    private boolean deleteItem(String itemId, Player player) {
        String ownerId = ownerId(itemId);
        try (MechaniumContentRegistrationScope scope = api.openContentScope(ownerId)) {
            // Committing an empty scope deletes only definitions owned by this custom item.
            scope.commit();
        } catch (RuntimeException exception) {
            player.sendMessage("§cNão foi possível remover o item: " + exception.getMessage());
            return false;
        }

        List<String> ids = configuredIds();
        ids.removeIf(id -> id.equalsIgnoreCase(itemId));
        plugin.getConfig().set(USER_ITEM_IDS_PATH, ids);
        plugin.saveConfig();
        player.sendMessage("§aItem personalizado removido: §f" + itemId);
        return true;
    }

    private void giveItem(Player player, ItemDefinition definition, int page) {
        ItemStack stack = api.createItemStack(definition.id(), 1).orElse(null);
        if (stack == null) {
            player.sendMessage("§cNão foi possível materializar este item.");
            openItem(player, definition, page);
            return;
        }
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.sendMessage("§aUma unidade de §f" + definition.id() + " §afoi entregue.");
        openItem(player, definition, page);
    }

    private void beginCreate(Player player) {
        drafts.put(player.getUniqueId(), new Draft());
        requestInput(player, ChatPrompt.ID,
                "§eCriação de item — digite um ID novo (ex.: copper_widget). Digite cancel para voltar.");
    }

    private void beginEdit(Player player, ItemDefinition definition, ChatPrompt prompt, String message) {
        if (!plugin.hasAdminAccess(player)) {
            player.sendMessage("§cSem permissão.");
            return;
        }
        editingItems.put(player.getUniqueId(), definition);
        requestInput(player, prompt, message + " Digite cancel para voltar.");
    }

    private void requestInput(Player player, ChatPrompt prompt, String message) {
        prompts.put(player.getUniqueId(), prompt);
        player.closeInventory();
        player.sendMessage(message);
    }

    @EventHandler
    public void onChatInput(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        ChatPrompt prompt = prompts.remove(playerId);
        if (prompt == null) return;
        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(javaPlugin,
                () -> handleInput(event.getPlayer(), prompt, message));
    }

    private void handleInput(Player player, ChatPrompt prompt, String raw) {
        if (!plugin.hasAdminAccess(player)) {
            drafts.remove(player.getUniqueId());
            prompts.remove(player.getUniqueId());
            player.sendMessage("§cSua permissão administrativa não está mais ativa.");
            return;
        }
        String value = raw == null ? "" : raw.trim();
        if (value.equalsIgnoreCase("cancel")) {
            drafts.remove(player.getUniqueId());
            ItemDefinition editing = editingItems.remove(player.getUniqueId());
            if (editing != null) openItem(player, editing, 0);
            else openList(player, 0);
            return;
        }
        if (prompt.isEdit()) {
            handleEditInput(player, prompt, value);
            return;
        }
        Draft draft = drafts.get(player.getUniqueId());
        if (draft == null) {
            player.sendMessage("§cA sessão de criação expirou.");
            openList(player, 0);
            return;
        }

        try {
            switch (prompt) {
                case ID -> {
                    String id = value.toLowerCase(Locale.ROOT);
                    if (!id.matches("[a-z0-9][a-z0-9_.-]{1,95}")) {
                        requestInput(player, ChatPrompt.ID,
                                "§cID inválido. Use 2–96 caracteres: letras minúsculas, números, _, - ou .");
                        return;
                    }
                    if (api.getItem(id).isPresent() || configuredIds().stream().anyMatch(id::equalsIgnoreCase)) {
                        requestInput(player, ChatPrompt.ID, "§cEsse ID já existe. Digite outro ID:");
                        return;
                    }
                    draft.id = id;
                    requestInput(player, ChatPrompt.DISPLAY_NAME,
                            "§eDigite o nome visível do item. Cores com & são aceitas:");
                }
                case DISPLAY_NAME -> {
                    if (value.isBlank() || value.length() > 64) {
                        requestInput(player, ChatPrompt.DISPLAY_NAME,
                                "§cO nome deve ter entre 1 e 64 caracteres. Digite novamente:");
                        return;
                    }
                    draft.displayName = value;
                    requestInput(player, ChatPrompt.MATERIAL,
                            "§eDigite um material Minecraft (ex.: COPPER_INGOT ou AMETHYST_SHARD):");
                }
                case MATERIAL -> {
                    Material material = Material.matchMaterial(value);
                    if (material == null || material.isAir() || !material.isItem()) {
                        requestInput(player, ChatPrompt.MATERIAL,
                                "§cMaterial inválido ou não é um item. Digite outro material:");
                        return;
                    }
                    draft.material = material.name();
                    requestInput(player, ChatPrompt.TECHNOLOGY,
                            "§eDigite o ID de uma pesquisa existente ou none para nenhuma:");
                }
                case TECHNOLOGY -> {
                    if (!value.equalsIgnoreCase("none")) {
                        TechnologyDefinition technology = api.getTechnology(value).orElse(null);
                        if (technology == null) {
                            requestInput(player, ChatPrompt.TECHNOLOGY,
                                    "§cPesquisa inexistente. Digite um ID existente ou none:");
                            return;
                        }
                        draft.requiredTechnologyId = technology.id();
                    } else {
                        draft.requiredTechnologyId = null;
                    }
                    requestInput(player, ChatPrompt.MAX_STACK,
                            "§eDigite a pilha máxima (1–64):");
                }
                case MAX_STACK -> {
                    int maxStack;
                    try {
                        maxStack = Integer.parseInt(value);
                    } catch (NumberFormatException exception) {
                        requestInput(player, ChatPrompt.MAX_STACK, "§cDigite um número entre 1 e 64:");
                        return;
                    }
                    if (maxStack < 1 || maxStack > 64) {
                        requestInput(player, ChatPrompt.MAX_STACK, "§cDigite um número entre 1 e 64:");
                        return;
                    }
                    draft.maxStackSize = maxStack;
                    saveDraft(player, draft);
                }
            }
        } catch (RuntimeException exception) {
            prompts.remove(player.getUniqueId());
            player.sendMessage("§cNão foi possível continuar: " + exception.getMessage());
            drafts.remove(player.getUniqueId());
            openList(player, 0);
        }
    }

    private void handleEditInput(Player player, ChatPrompt prompt, String value) {
        ItemDefinition current = editingItems.get(player.getUniqueId());
        if (current == null) {
            player.sendMessage("§cA sessão de edição expirou.");
            openList(player, 0);
            return;
        }

        ItemDefinition updated;
        try {
            String displayName = current.displayName();
            String material = current.material();
            String technology = current.requiredTechnologyId();
            int maxStack = current.maxStackSize();

            switch (prompt) {
                case EDIT_DISPLAY_NAME -> {
                    if (value.isBlank() || value.length() > 64) {
                        requestInput(player, prompt, "§cO nome deve ter entre 1 e 64 caracteres. Digite novamente:");
                        return;
                    }
                    displayName = value;
                }
                case EDIT_MATERIAL -> {
                    Material selected = Material.matchMaterial(value);
                    if (selected == null || selected.isAir() || !selected.isItem()) {
                        requestInput(player, prompt, "§cMaterial inválido. Digite um material que seja item:");
                        return;
                    }
                    material = selected.name();
                }
                case EDIT_TECHNOLOGY -> {
                    if (value.equalsIgnoreCase("none")) {
                        technology = null;
                    } else {
                        TechnologyDefinition selected = api.getTechnology(value).orElse(null);
                        if (selected == null) {
                            requestInput(player, prompt, "§cPesquisa inexistente. Digite um ID existente ou none:");
                            return;
                        }
                        technology = selected.id();
                    }
                }
                case EDIT_MAX_STACK -> {
                    try {
                        maxStack = Integer.parseInt(value);
                    } catch (NumberFormatException exception) {
                        requestInput(player, prompt, "§cDigite um número entre 1 e 64:");
                        return;
                    }
                    if (maxStack < 1 || maxStack > 64) {
                        requestInput(player, prompt, "§cDigite um número entre 1 e 64:");
                        return;
                    }
                }
                default -> throw new IllegalArgumentException("Prompt de edição inválido.");
            }

            updated = new ItemDefinition(current.id(), displayName, material, current.lore(),
                    current.customModelData(), maxStack, current.categoryId(), technology);
            if (!api.overrideItem(updated)) {
                throw new IllegalStateException("O item deixou de estar registrado.");
            }
        } catch (RuntimeException exception) {
            player.sendMessage("§cAlteração não aplicada: " + exception.getMessage());
            editingItems.remove(player.getUniqueId());
            openItem(player, current, 0);
            return;
        }

        editingItems.remove(player.getUniqueId());
        player.sendMessage("§aItem atualizado: §f" + updated.id());
        openItem(player, updated, 0);
    }

    private void saveDraft(Player player, Draft draft) {
        if (api.getCategory(CATEGORY_ID).isEmpty()) {
            drafts.remove(player.getUniqueId());
            player.sendMessage("§cA categoria base do editor não está registrada. Reative o módulo Slime Customizer.");
            openList(player, 0);
            return;
        }

        ItemDefinition definition = new ItemDefinition(
                draft.id, draft.displayName, draft.material,
                List.of("&8Criado na oficina de personalização MECHANIUM"),
                0, draft.maxStackSize, CATEGORY_ID, draft.requiredTechnologyId);

        List<String> ids = configuredIds();
        if (!ids.contains(definition.id())) ids.add(definition.id());
        plugin.getConfig().set(USER_ITEM_IDS_PATH, ids);
        plugin.saveConfig();

        try (MechaniumContentRegistrationScope scope = api.openContentScope(ownerId(definition.id()))) {
            if (!api.registerItem(definition)) {
                throw new IllegalStateException("Já existe uma definição diferente com esse ID.");
            }
            scope.commit();
        } catch (RuntimeException exception) {
            ids.removeIf(id -> id.equalsIgnoreCase(definition.id()));
            plugin.getConfig().set(USER_ITEM_IDS_PATH, ids);
            plugin.saveConfig();
            player.sendMessage("§cFalha ao salvar o item: " + exception.getMessage());
            drafts.remove(player.getUniqueId());
            openList(player, 0);
            return;
        }

        drafts.remove(player.getUniqueId());
        player.sendMessage("§aItem personalizado registrado: §f" + definition.id());
        giveItem(player, definition, 0);
        openList(player, 0);
    }

    private List<ItemDefinition> configuredItems() {
        return configuredIds().stream()
                .map(api::getItem)
                .flatMap(java.util.Optional::stream)
                .sorted(Comparator.comparing(ItemDefinition::id))
                .toList();
    }

    private List<String> configuredIds() {
        return new ArrayList<>(plugin.getConfig().getStringList(USER_ITEM_IDS_PATH).stream()
                .filter(Objects::nonNull)
                .map(id -> id.trim().toLowerCase(Locale.ROOT))
                .filter(id -> id.matches("[a-z0-9][a-z0-9_.-]{1,95}"))
                .distinct()
                .toList());
    }

    static String ownerId(String itemId) {
        return "customizer:item:" + itemId.toLowerCase(Locale.ROOT);
    }

    private void open(Player player, MenuHolder holder) {
        player.openInventory(holder.getInventory());
    }

    private MenuHolder create(String title) {
        return new MenuHolder(Bukkit.createInventory(null, SIZE, legacy(title)));
    }

    private void fillFrame(Inventory inventory) {
        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) inventory.setItem(slot, pane);
        }
    }

    private void button(MenuHolder holder, int slot, Material material, String name,
                        List<String> lore, Consumer<Player> action) {
        holder.getInventory().setItem(slot, item(material, name, lore));
        holder.actions.put(slot, action);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        setDisplay(stack, name, lore);
        return stack;
    }

    private void setDisplay(ItemStack stack, String name, List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;
        meta.displayName(legacy(name));
        meta.lore(lore.stream().map(this::legacy).toList());
        stack.setItemMeta(meta);
    }

    private Component legacy(String value) {
        return LegacyComponentSerializer.legacySection().deserialize(value);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        Consumer<Player> action = holder.actions.get(event.getSlot());
        if (action != null) action.accept(player);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) event.setCancelled(true);
    }

    private enum ChatPrompt {
        ID, DISPLAY_NAME, MATERIAL, TECHNOLOGY, MAX_STACK,
        EDIT_DISPLAY_NAME, EDIT_MATERIAL, EDIT_TECHNOLOGY, EDIT_MAX_STACK;

        private boolean isEdit() {
            return name().startsWith("EDIT_");
        }
    }

    private static final class Draft {
        private String id;
        private String displayName;
        private String material;
        private String requiredTechnologyId;
        private int maxStackSize;
    }

    private static final class MenuHolder implements InventoryHolder {
        private final Inventory inventory;
        private final Map<Integer, Consumer<Player>> actions = new HashMap<>();

        private MenuHolder(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override public Inventory getInventory() { return inventory; }
    }
}
