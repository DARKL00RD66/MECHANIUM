package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.api.model.ResearchProgress;
import dev.mechanium.api.model.TechnologyDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.function.Consumer;

/** Chest-based interface and per-era progression tree. No client mod is required. */
final class MechaniumResearchMenu implements Listener {
    private static final int SIZE = 54;
    private static final int[] NODE_SLOTS = {10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43};
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
    };

    private final MechaniumPlugin plugin;
    private final MechaniumApi api;
    private final NamespacedKey guideKey;
    private final MechaniumFabricationMenu fabricationMenu;
    private final MechaniumWikiMenu wikiMenu;
    private final EmcExchangeManager emcExchangeManager;

    MechaniumResearchMenu(MechaniumPlugin plugin, MechaniumApi api, EmcExchangeManager emcExchangeManager) {
        this.plugin = plugin;
        this.api = api;
        this.guideKey = new NamespacedKey(plugin, "guide_book");
        this.fabricationMenu = new MechaniumFabricationMenu(plugin, api);
        this.wikiMenu = new MechaniumWikiMenu(plugin, api);
        this.emcExchangeManager = emcExchangeManager;
    }

    void open(Player player) { openHome(player); }

    void giveGuide(Player player) {
        ItemStack guide = createGuideBook();
        if (player.getInventory().containsAtLeast(guide, 1)) return;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(guide);
        if (!leftovers.isEmpty()) {
            // Never drop the guide when the inventory is full; retry after the player frees a slot.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) giveGuide(player);
            }, 20L);
            if (player.isOnline()) player.sendMessage("§eO inventário está cheio. O Guia MECHANIUM será entregue quando houver espaço.");
        }
    }

    private ItemStack createGuideBook() {
        ItemStack book = new ItemStack(Material.BOOK);
        ItemMeta meta = book.getItemMeta();
        meta.displayName(Component.text("Guia do MECHANIUM", net.kyori.adventure.text.format.NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Clique com o botão direito para abrir."),
                Component.text("Pesquisas, máquinas e progresso histórico."),
                Component.text("Item vinculado ao jogador: não pode ser movido ou descartado.")
        ));
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(guideKey, PersistentDataType.BYTE, (byte) 1);
        book.setItemMeta(meta);
        return book;
    }

    @EventHandler
    public void onGuideUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack held = event.getItem();
        if (held == null || !held.hasItemMeta()) return;
        Byte marker = held.getItemMeta().getPersistentDataContainer().get(guideKey, PersistentDataType.BYTE);
        if (marker == null || marker != (byte) 1) return;
        event.setCancelled(true);
        open(event.getPlayer());
    }

    private void openHome(Player player) {
        MenuHolder holder = create("MECHANIUM • Central");
        Inventory inv = holder.getInventory();
        fillFrame(inv);
        inv.setItem(4, item(Material.NETHER_STAR, "§6§lMECHANIUM", List.of(
                "§7Sua era atual: §f" + currentEra(player),
                "§7Pontos científicos: §b" + api.getResearchPoints(player.getUniqueId()),
                "§7Tecnologias pesquisadas: §a" + unlockedCount(player) + "§7/§f" + api.getTechnologies().size(),
                "§7Saldo EMC: §b" + (emcExchangeManager == null ? "desativado" : emcExchangeManager.balance(player.getUniqueId()) + " EMC"),
                "§8Escolha uma área para continuar."
        )));
        button(holder, 20, Material.KNOWLEDGE_BOOK, "§eÁrvore de progressão",
                List.of("§7Abre a árvore na sua era atual.", "§7Veja pesquisas, custos e pré-requisitos."),
                this::openCurrentEraTree);
        button(holder, 22, Material.CLOCK, "§dEras históricas",
                List.of("§7Veja a linha do tempo completa.", "§7Selecione um relógio para consultar uma era."),
                this::openEraSelector);
        button(holder, 24, Material.CRAFTING_TABLE, "§bCrafting",
                List.of("§7Fabrique itens e máquinas.", "§7Os ingredientes são consumidos somente aqui."),
                fabricationMenu::open);
        button(holder, 31, Material.BOOK, "§6Wiki",
                List.of("§7Consulte itens, receitas e máquinas.", "§7A Wiki não consome ingredientes."),
                p -> wikiMenu.openHome(p, this::openHome));
        if (emcExchangeManager != null) {
            button(holder, 40, Material.EMERALD, "§aCarteira EMC", List.of(
                    "§7Seu saldo: §b" + emcExchangeManager.balance(player.getUniqueId()) + " EMC",
                    "§7Sistema individual de equivalência.",
                    "§7Itens aprendidos por você: §f" + emcExchangeManager.learnedCount(player.getUniqueId()),
                    "§eClique para abrir seu ledger EMC."
            ), emcExchangeManager::openCommand);
        } else {
            inv.setItem(40, item(Material.GRAY_DYE, "§7Carteira EMC desativada",
                    List.of("§8O módulo emc-exchange está desativado.")));
        }
        button(holder, 49, Material.BARRIER, "§cFechar", List.of(), Player::closeInventory);
        animateOpen(player, holder);
    }

    private void openCurrentEraTree(Player player) {
        List<EraDefinition> eras = api.getEras().stream()
                .sorted(Comparator.comparingInt(EraDefinition::order)).toList();
        String currentId = api.getPlayerEra(player.getUniqueId())
                .orElseGet(() -> api.getServerEra().orElse(""));
        int index = 0;
        for (int i = 0; i < eras.size(); i++) {
            if (eras.get(i).id().equalsIgnoreCase(currentId)) {
                index = i;
                break;
            }
        }
        openEraTree(player, index, 0);
    }

    /**
     * Era selector is the entry point to the progression tree. Each clock opens
     * the corresponding era directly; the Nether Star above the active era marks
     * the player's effective era without changing their progression state.
     */
    private void openEraSelector(Player player) {
        List<EraDefinition> eras = api.getEras().stream()
                .sorted(Comparator.comparingInt(EraDefinition::order)).toList();
        if (eras.isEmpty()) {
            player.sendMessage("§cNenhuma era foi configurada.");
            return;
        }

        String currentId = api.getPlayerEra(player.getUniqueId())
                .orElseGet(() -> api.getServerEra().orElse(""));
        EraDefinition current = eras.stream()
                .filter(era -> era.id().equalsIgnoreCase(currentId))
                .findFirst().orElse(eras.getFirst());

        long totalUnlocked = unlockedCount(player);
        MenuHolder holder = create("MECHANIUM • Eras históricas");
        Inventory inv = holder.getInventory();
        fillFrame(inv);
        inv.setItem(4, item(Material.NETHER_STAR, "§6§lERA ATUAL: " + current.displayName(),
                List.of("§7Pontos científicos: §b" + api.getResearchPoints(player.getUniqueId()),
                        "§7Tecnologias pesquisadas: §a" + totalUnlocked + "§7/§f" + api.getTechnologies().size(),
                        "§8A estrela do Nether marca sua era atual.",
                        "§8Selecione um relógio para abrir a árvore daquela era.")));

        int[] columns = {1, 3, 5, 7};
        for (int index = 0; index < eras.size() && index < 12; index++) {
            EraDefinition era = eras.get(index);
            int column = columns[index % columns.length];
            int group = index / columns.length;
            int starSlot = (group * 2) * 9 + column;
            int clockSlot = (group * 2 + 1) * 9 + column;
            boolean isCurrent = era.id().equalsIgnoreCase(current.id());

            inv.setItem(starSlot, item(isCurrent ? Material.NETHER_STAR : Material.GRAY_STAINED_GLASS_PANE,
                    isCurrent ? "§6§lERA ATUAL" : "§8 ", isCurrent
                            ? List.of("§7" + era.displayName(), "§eVocê está nesta era.")
                            : List.of()));

            List<TechnologyDefinition> eraTechnologies = api.getTechnologies().stream()
                    .filter(technology -> technology.requiredEraId().equals(era.id())).toList();
            long researched = eraTechnologies.stream()
                    .filter(technology -> api.hasUnlockedTechnology(player.getUniqueId(), technology.id())).count();
            long researchable = eraTechnologies.stream()
                    .filter(technology -> !api.hasUnlockedTechnology(player.getUniqueId(), technology.id()))
                    .filter(technology -> current.order() >= era.order())
                    .filter(technology -> technology.prerequisites().stream()
                            .allMatch(id -> api.hasUnlockedTechnology(player.getUniqueId(), id))).count();

            String title = (isCurrent ? "§6★ " : "§e") + "Era " + (era.order() + 1)
                    + " • " + era.displayName();
            inv.setItem(clockSlot, item(Material.CLOCK, title, List.of(
                    "§7Tecnologias nesta era: §f" + eraTechnologies.size(),
                    "§7Pesquisadas: §a" + researched,
                    "§7Disponíveis para iniciar: §e" + researchable,
                    "§7Ainda bloqueadas: §c" + (eraTechnologies.size() - researched - researchable),
                    isCurrent ? "§6ERA ATUAL" : "§aClique para abrir esta era."
            )));
            int selectedEraIndex = index;
            holder.actions.put(clockSlot, p -> openEraTree(p, selectedEraIndex, 0));
        }

        button(holder, 49, Material.ARROW, "§eVoltar ao menu principal", List.of(), this::openHome);
        animateOpen(player, holder);
    }

    private void openEraTree(Player player, int requestedEra, int requestedPage) {
        List<EraDefinition> eras = api.getEras().stream()
                .sorted(Comparator.comparingInt(EraDefinition::order)).toList();
        if (eras.isEmpty()) {
            player.sendMessage("§cNenhuma era foi configurada.");
            return;
        }
        int eraIndex = Math.max(0, Math.min(requestedEra, eras.size() - 1));
        EraDefinition era = eras.get(eraIndex);
        plugin.processDueResearches(player, System.currentTimeMillis());
        Map<String, ResearchProgress> activeResearches = new HashMap<>();
        api.getResearches(player.getUniqueId()).forEach(research ->
                activeResearches.put(research.technologyId(), research));
        List<TechnologyDefinition> technologies = api.getTechnologies().stream()
                .filter(t -> t.requiredEraId().equals(era.id()))
                .sorted(Comparator.comparingInt((TechnologyDefinition t) -> t.prerequisites().size())
                        .thenComparing(TechnologyDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int pageCount = Math.max(1, (technologies.size() + NODE_SLOTS.length - 1) / NODE_SLOTS.length);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        int start = page * NODE_SLOTS.length;

        MenuHolder holder = create("MECHANIUM • " + era.displayName());
        Inventory inv = holder.getInventory();
        fillFrame(inv);
        inv.setItem(4, item(Material.MAP, "§6§l" + era.displayName(), List.of(
                "§7Era " + (eraIndex + 1) + " de " + eras.size(),
                "§7Tecnologias: §f" + technologies.size(),
                "§7Página: §f" + (page + 1) + "/" + pageCount,
                "§7Pontos: §b" + api.getResearchPoints(player.getUniqueId()),
                "§8Verde: concluída • Amarelo: pesquisável • Cinza: bloqueada"
        )));

        for (int slot : new int[]{11, 13, 15, 20, 22, 24, 29, 31, 33, 38, 40, 42}) {
            inv.setItem(slot, item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, "§8·", List.of()));
        }

        int count = Math.min(NODE_SLOTS.length, technologies.size() - start);
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            TechnologyDefinition technology = technologies.get(start + i);
            int slot = NODE_SLOTS[i];
            ResearchProgress active = activeResearches.get(technology.id());
            holder.technologySlots.put(slot, technology);
            inv.setItem(slot, technologyItem(player, technology, era, active, now));
            holder.actions.put(slot, p -> {
                if (!api.hasUnlockedTechnology(p.getUniqueId(), technology.id())) {
                    plugin.startResearch(p, technology);
                } else {
                    p.sendMessage("§aPesquisa já concluída: §f" + technology.displayName());
                }
                openEraTree(p, eraIndex, page);
            });
        }

        if (page > 0) button(holder, 45, Material.PAPER, "§ePágina anterior", List.of(),
                p -> openEraTree(p, eraIndex, page - 1));
        if (eraIndex > 0) button(holder, 47, Material.ARROW, "§eEra anterior", List.of(),
                p -> openEraTree(p, eraIndex - 1, 0));
        button(holder, 48, Material.BOOK, "§fResumo",
                List.of("§7" + unlockedCount(player) + " pesquisas concluídas"), this::openHome);
        button(holder, 49, Material.COMPASS, "§6Menu principal", List.of(), this::openHome);
        if (eraIndex + 1 < eras.size()) button(holder, 51, Material.ARROW, "§ePróxima era", List.of(),
                p -> openEraTree(p, eraIndex + 1, 0));
        if (page + 1 < pageCount) button(holder, 53, Material.PAPER, "§ePróxima página", List.of(),
                p -> openEraTree(p, eraIndex, page + 1));
        animateOpen(player, holder);
    }


    private ItemStack technologyItem(Player player, TechnologyDefinition technology, EraDefinition era,
                                     ResearchProgress active, long nowEpochMillis) {
        boolean unlocked = api.hasUnlockedTechnology(player.getUniqueId(), technology.id());
        boolean eraAllowed = api.getPlayerEra(player.getUniqueId()).flatMap(api::getEra)
                .map(current -> current.order() >= era.order()).orElse(false);
        boolean prerequisitesMet = technology.prerequisites().stream()
                .allMatch(id -> api.hasUnlockedTechnology(player.getUniqueId(), id));

        List<String> lore = new ArrayList<>();
        lore.add("§7ID: §f" + technology.id());
        lore.add("§7Custo: §b" + technology.researchCost() + " pontos");
        if (unlocked) {
            lore.add("§7Estado: §aConcluída");
            lore.add("§aPesquisa concluída.");
            return item(Material.LIME_DYE, "§a" + technology.displayName(), lore);
        }

        if (active != null) {
            double fraction = active.progressFraction(nowEpochMillis);
            int percent = (int) Math.round(fraction * 100.0d);
            int filled = (int) Math.round(fraction * 10.0d);
            StringBuilder bar = new StringBuilder("§a");
            for (int i = 0; i < 10; i++) bar.append(i < filled ? "▰" : "§8▱");
            lore.add("§7Estado: §dPesquisando");
            lore.add("§7Progresso: " + bar + " §f" + percent + "%");
            lore.add("§7Tempo restante: §b" + plugin.formatResearchDuration(
                    active.remainingMillis(nowEpochMillis)));
            lore.add("§8O cronômetro continua offline.");
            return item(Material.CLOCK, "§d" + technology.displayName(), lore);
        }

        lore.add("§7Estado: " + (eraAllowed && prerequisitesMet ? "§eDisponível" : "§cBloqueada"));
        lore.add("§7Duração estimada: §b" + plugin.formatResearchDuration(
                plugin.researchDurationMillis(technology)));
        lore.add("§8Mínimo de 2 minutos; conta offline.");
        if (!technology.prerequisites().isEmpty()) {
            lore.add("§7Pré-requisitos:");
            technology.prerequisites().forEach(id -> lore.add(" §8• §f" + id
                    + (api.hasUnlockedTechnology(player.getUniqueId(), id) ? " §a✓" : " §c✗")));
        }
        lore.add(eraAllowed && prerequisitesMet
                ? "§eClique para iniciar a pesquisa."
                : "§cAtenda aos requisitos para pesquisar.");
        Material icon = eraAllowed && prerequisitesMet ? Material.YELLOW_DYE : Material.GRAY_DYE;
        return item(icon, "§e" + technology.displayName(), lore);
    }

    /** Refreshes countdowns in an open research-tree inventory without replaying its opening animation. */
    void refreshProgress(Player player, long nowEpochMillis) {
        if (!player.isOnline()
                || !(player.getOpenInventory().getTopInventory().getHolder() instanceof MenuHolder holder)
                || holder.animating || holder.technologySlots.isEmpty()) return;

        Map<String, ResearchProgress> activeResearches = new HashMap<>();
        api.getResearches(player.getUniqueId()).forEach(research ->
                activeResearches.put(research.technologyId(), research));
        for (Map.Entry<Integer, TechnologyDefinition> entry : holder.technologySlots.entrySet()) {
            TechnologyDefinition technology = entry.getValue();
            ResearchProgress active = activeResearches.get(technology.id());
            EraDefinition era = api.getEra(technology.requiredEraId()).orElse(null);
            if (era != null) {
                holder.inventory.setItem(entry.getKey(),
                        technologyItem(player, technology, era, active, nowEpochMillis));
            }
        }
    }

    private void openSimpleList(Player player, String section) {
        MenuHolder holder = create("MECHANIUM • " + section);
        Inventory inv = holder.getInventory();
        fillFrame(inv);
        int index = 0;
        if (section.equals("itens")) {
            for (var entry : api.getItems().stream().limit(CONTENT_SLOTS.length).toList()) {
                Material material = Material.matchMaterial(entry.material());
                inv.setItem(CONTENT_SLOTS[index++], item(material == null ? Material.BARRIER : material,
                        "§e" + entry.displayName(), List.of("§7ID: §f" + entry.id(),
                                "§7Categoria: §f" + entry.categoryId(),
                                "§7Tecnologia: §f" + Objects.toString(entry.requiredTechnologyId(), "Nenhuma"))));
            }
        } else if (section.equals("receitas")) {
            for (var entry : api.getRecipes().stream().limit(CONTENT_SLOTS.length).toList()) {
                inv.setItem(CONTENT_SLOTS[index++], item(Material.PAPER, "§b" + entry.id(),
                        List.of("§7Resultado: §f" + entry.resultItemId(),
                                "§7Tecnologia: §f" + Objects.toString(entry.requiredTechnologyId(), "Nenhuma"))));
            }
        } else {
            for (var entry : api.getMachines().stream().limit(CONTENT_SLOTS.length).toList()) {
                inv.setItem(CONTENT_SLOTS[index++], item(Material.FURNACE, "§6" + entry.displayName(),
                        List.of("§7Bloco: §f" + entry.blockMaterial(), "§7Entrada: §f" + entry.inputId(),
                                "§7Saída: §f" + entry.outputItemId())));
            }
        }
        button(holder, 49, Material.ARROW, "§eVoltar", List.of(), this::openHome);
        animateOpen(player, holder);
    }

    private MenuHolder create(String title) {
        MenuHolder holder = new MenuHolder();
        holder.inventory = Bukkit.createInventory(holder, SIZE, Component.text(title));
        return holder;
    }

    private void animateOpen(Player player, MenuHolder holder) {
        holder.animating = true;
        MechaniumMenuAnimator.open(plugin, player, holder.inventory, () -> holder.animating = false);
    }

    private void fillFrame(Inventory inventory) {
        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) inventory.setItem(slot, pane);
        }
    }

    private void button(MenuHolder holder, int slot, Material material, String name, List<String> lore,
                        Consumer<Player> action) {
        holder.inventory.setItem(slot, item(material, name, lore));
        holder.actions.put(slot, action);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
        if (!lore.isEmpty()) meta.lore(lore.stream().map(LegacyComponentSerializer.legacySection()::deserialize).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    private boolean isGuide(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        Byte marker = stack.getItemMeta().getPersistentDataContainer().get(guideKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    @EventHandler(ignoreCancelled = true)
    public void onGuideMove(InventoryClickEvent event) {
        if (isGuide(event.getCurrentItem()) || isGuide(event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        if (event.getHotbarButton() >= 0 && event.getWhoClicked() instanceof Player player
                && isGuide(player.getInventory().getItem(event.getHotbarButton()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onGuideDrag(InventoryDragEvent event) {
        if (isGuide(event.getOldCursor())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGuideDrop(PlayerDropItemEvent event) {
        if (isGuide(event.getItemDrop().getItemStack())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onGuideSwap(PlayerSwapHandItemsEvent event) {
        if (isGuide(event.getMainHandItem()) || isGuide(event.getOffHandItem())) event.setCancelled(true);
    }

    @EventHandler
    public void onGuideDeath(PlayerDeathEvent event) {
        event.getDrops().removeIf(this::isGuide);
    }

    @EventHandler
    public void onGuideRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (event.getPlayer().isOnline()) giveGuide(event.getPlayer());
        });
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (holder.animating) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        Consumer<Player> action = holder.actions.get(event.getSlot());
        if (action != null) action.accept(player);
    }

    private String currentEra(Player player) {
        return api.getPlayerEra(player.getUniqueId()).flatMap(api::getEra)
                .map(EraDefinition::displayName).orElse("Não definida");
    }

    private long unlockedCount(Player player) {
        return api.getTechnologies().stream()
                .filter(technology -> api.hasUnlockedTechnology(player.getUniqueId(), technology.id())).count();
    }

    private static final class MenuHolder implements InventoryHolder {
        private Inventory inventory;
        private boolean animating;
        private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
        private final Map<Integer, TechnologyDefinition> technologySlots = new HashMap<>();
        @Override public Inventory getInventory() { return inventory; }
    }
}
