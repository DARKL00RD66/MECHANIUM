package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.EraDefinition;
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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Consumer;

/** Admin-only chest HUD for historical progression, online player eras and vanilla recipe gates. */
final class MechaniumAdminMenu implements Listener {
    private static final int SIZE = 54;
    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
    };

    private final JavaPlugin plugin;
    private final MechaniumPlugin mechanium;
    private final MechaniumApi api;

    MechaniumAdminMenu(MechaniumPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.mechanium = plugin;
        this.api = api;
    }

    void open(Player player) {
        if (!mechanium.hasAdminAccess(player)) {
            player.sendMessage("§cVocê não tem permissão para abrir o painel administrativo.");
            return;
        }
        openHome(player);
    }

    private void openHome(Player player) {
        AdminHolder holder = create("MECHANIUM • Administração");
        Inventory inv = holder.getInventory();
        fillFrame(inv);
        inv.setItem(4, item(Material.NETHER_STAR, "§6§lPainel administrativo", List.of(
                "§7Era global do servidor: §e" + mechanium.serverEraName(),
                "§7Eras cadastradas: §f" + api.getEras().size(),
                "§7Tecnologias: §f" + api.getTechnologies().size(),
                "§7Receitas vanilla configuradas: §f" + configuredRecipeCount(),
                "§7Receitas vanilla detectadas: §f" + VanillaRecipeCatalog.discover().size(),
                "§7Era inicial: §e" + defaultEraName()
        )));
        button(holder, 20, Material.PLAYER_HEAD, "§eEras individuais (testes)",
                List.of("§7Ajuste a era de um jogador por vez.", "§7Sobrescrita isolada; não altera o servidor."), this::openPlayerTargets);
        button(holder, 22, Material.CRAFTING_TABLE, "§bReceitas vanilla por era",
                List.of("§7Defina a era exigida para receitas do Minecraft.", "§7Clique numa receita para avançar a era.", "§7Ao passar da última era, o bloqueio é removido."),
                this::openVanillaRecipes);
        button(holder, 24, Material.CLOCK, "§aEra inicial de configuração",
                List.of("§7Fallback usado apenas quando o banco", "§7ainda não possui uma era global definida."),
                this::openDefaultEraSelector);
        button(holder, 26, Material.NETHER_STAR, "§6§lTrocar era GLOBAL",
                List.of("§7Era atual: §e" + mechanium.serverEraName(),
                        "§7Altera a progressão para todo o servidor.",
                        "§7Jogadores online recebem título e som.",
                        "§7Jogadores offline recebem o aviso ao entrar."),
                this::openGlobalEraSelector);
        button(holder, 29, Material.BOOK, "§bReceitas MECHANIUM",
                List.of("§7Editar receitas dos módulos e addons.", "§7Resultado, quantidade, ingredientes e tecnologia."),
                mechanium::openRecipeAdminMenu);
        button(holder, 31, Material.EXPERIENCE_BOTTLE, "§dPontos de pesquisa",
                List.of("§7Selecione um jogador para conceder", "§750 pontos científicos."), this::openResearchPointTargets);
        boolean gatesEnabled = plugin.getConfig().getBoolean("vanilla-recipe-gates.enabled", true);
        button(holder, 40, gatesEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
                gatesEnabled ? "§aBloqueios vanilla: ATIVADOS" : "§cBloqueios vanilla: DESATIVADOS",
                List.of("§7Clique para alternar o sistema global.", "§7As regras individuais continuam salvas."),
                p -> {
                    plugin.getConfig().set("vanilla-recipe-gates.enabled", !gatesEnabled);
                    plugin.saveConfig();
                    openHome(p);
                });
        button(holder, 49, Material.BARRIER, "§cFechar", List.of(), Player::closeInventory);
        animateOpen(player, holder);
    }

    private void openPlayerTargets(Player admin) {
        AdminHolder holder = create("MECHANIUM • Jogadores");
        Inventory inv = holder.getInventory();
        fillFrame(inv);
        inv.setItem(4, item(Material.PLAYER_HEAD, "§eSobrescritas individuais",
                List.of("§7Use este menu para testar progressões", "§7em uma era sem mudar a era global.")));
        button(holder, 49, Material.ARROW, "§eVoltar", List.of(), this::openHome);
        button(holder, 45, Material.COMPASS, "§6Testar era em todos online",
                List.of("§7Aplica uma sobrescrita de teste", "§7a todos que estão conectados."), p -> openEraSelector(p, null, "todos os jogadores online"));
        int index = 0;
        for (Player target : Bukkit.getOnlinePlayers().stream()
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(CONTENT_SLOTS.length).toList()) {
            int slot = CONTENT_SLOTS[index++];
            String era = currentEraName(target);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof SkullMeta skull) skull.setOwningPlayer(target);
            meta.displayName(Component.text("§e" + target.getName()));
            meta.lore(List.of(Component.text("§7Era atual: §f" + era),
                    Component.text("§aClique para escolher uma era.")));
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            holder.actions.put(slot, p -> openEraSelector(p, target.getUniqueId(), target.getName()));
        }
        animateOpen(admin, holder);
    }

    private void openEraSelector(Player admin, UUID targetId, String targetName) {
        List<EraDefinition> eras = orderedEras();
        AdminHolder holder = create("MECHANIUM • Definir era");
        Inventory inv = holder.getInventory();
        fillFrame(inv);
        inv.setItem(4, item(Material.MAP, "§6Escolher era", List.of(
                "§7Alvo: §f" + targetName,
                "§7Clique na era que deseja aplicar."
        )));
        button(holder, 49, Material.ARROW, "§eVoltar", List.of(), this::openPlayerTargets);
        button(holder, 45, Material.COMPASS, "§aHerdar era global",
                List.of("§7Remove a sobrescrita de teste.", "§7O jogador volta a acompanhar a era do servidor."),
                p -> {
                    if (targetId == null) {
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            api.clearPlayerEraOverride(online.getUniqueId());
                        }
                        p.sendMessage("§aSobrescritas removidas. Todos os jogadores online voltaram a herdar a era global.");
                    } else {
                        api.clearPlayerEraOverride(targetId);
                        String globalName = mechanium.serverEraName();
                        p.sendMessage("§aSobrescrita removida de §f" + targetName
                                + "§a. Agora herda: §e" + globalName + "§a.");
                    }
                    openPlayerTargets(p);
                });
        int index = 0;
        for (EraDefinition era : eras) {
            if (index >= CONTENT_SLOTS.length) break;
            int slot = CONTENT_SLOTS[index++];
            button(holder, slot, Material.CLOCK, "§e" + era.displayName(),
                    List.of("§7ID: §f" + era.id(), "§7Ordem histórica: §f" + era.order()),
                    p -> {
                        if (targetId == null) {
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                api.setPlayerEra(online.getUniqueId(), era.id());
                            }
                            p.sendMessage("§aEra de todos os jogadores online alterada para §f" + era.displayName() + "§a.");
                        } else {
                            Player target = Bukkit.getPlayer(targetId);
                            if (target == null) {
                                p.sendMessage("§cO jogador não está mais conectado.");
                            } else {
                                api.setPlayerEra(targetId, era.id());
                                p.sendMessage("§aEra de §f" + target.getName() + " §aalterada para §f" + era.displayName() + "§a.");
                            }
                        }
                        openHome(p);
                    });
        }
        animateOpen(admin, holder);
    }

    private void openGlobalEraSelector(Player admin) {
        List<EraDefinition> eras = orderedEras();
        AdminHolder holder = create("MECHANIUM • Era global");
        Inventory inv = holder.getInventory();
        fillFrame(inv);
        inv.setItem(4, item(Material.NETHER_STAR, "§6§lEra global do servidor", List.of(
                "§7Era ativa: §e" + mechanium.serverEraName(),
                "§7A próxima escolha será aplicada como",
                "§7progressão padrão para todos os jogadores."
        )));
        button(holder, 49, Material.ARROW, "§eVoltar", List.of(), this::openHome);
        int index = 0;
        for (EraDefinition era : eras) {
            if (index >= CONTENT_SLOTS.length) break;
            int slot = CONTENT_SLOTS[index++];
            boolean current = api.getServerEra().map(era.id()::equalsIgnoreCase).orElse(false);
            button(holder, slot, current ? Material.LIME_DYE : Material.CLOCK,
                    (current ? "§a" : "§e") + era.displayName(),
                    List.of("§7ID: §f" + era.id(), "§7Ordem histórica: §f" + era.order(),
                            current ? "§aEra global atual" : "§eClique para tornar global"),
                    p -> {
                        if (!current) {
                            mechanium.changeServerEra(era.id());
                            p.sendMessage("§aNova era global do servidor: §f" + era.displayName()
                                    + "§a. Todos os jogadores herdam esta era; testes individuais permanecem intactos.");
                        } else {
                            p.sendMessage("§eEsta já é a era global ativa.");
                        }
                        openHome(p);
                    });
        }
        animateOpen(admin, holder);
    }

    private void openDefaultEraSelector(Player admin) {
        List<EraDefinition> eras = orderedEras();
        AdminHolder holder = create("MECHANIUM • Era inicial");
        Inventory inv = holder.getInventory();
        fillFrame(inv);
        inv.setItem(4, item(Material.CLOCK, "§aEra inicial de configuração",
                List.of("§7Usada como fallback quando o banco", "§7ainda não possui uma era global.")));
        button(holder, 49, Material.ARROW, "§eVoltar", List.of(), this::openHome);
        int index = 0;
        for (EraDefinition era : eras) {
            if (index >= CONTENT_SLOTS.length) break;
            int slot = CONTENT_SLOTS[index++];
            button(holder, slot, Material.CLOCK, "§e" + era.displayName(),
                    List.of("§7ID: §f" + era.id(), "§7Ordem: §f" + era.order()),
                    p -> {
                        plugin.getConfig().set("progression.default-era", era.id());
                        plugin.saveConfig();
                        p.sendMessage("§aEra inicial definida como §f" + era.displayName() + "§a.");
                        openHome(p);
                    });
        }
        animateOpen(admin, holder);
    }

    private void openResearchPointTargets(Player admin) {
        AdminHolder holder = create("MECHANIUM • Pontos científicos");
        Inventory inv = holder.getInventory();
        fillFrame(inv);
        inv.setItem(4, item(Material.EXPERIENCE_BOTTLE, "§dConceder pontos",
                List.of("§7Clique no jogador para conceder 50 pontos.")));
        button(holder, 49, Material.ARROW, "§eVoltar", List.of(), this::openHome);
        int index = 0;
        for (Player target : Bukkit.getOnlinePlayers().stream()
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(CONTENT_SLOTS.length).toList()) {
            int slot = CONTENT_SLOTS[index++];
            button(holder, slot, Material.EXPERIENCE_BOTTLE, "§d" + target.getName(),
                    List.of("§7Pontos atuais: §f" + api.getResearchPoints(target.getUniqueId()),
                            "§aClique para conceder 50 pontos."),
                    p -> {
                        api.addResearchPoints(target.getUniqueId(), 50);
                        p.sendMessage("§a50 pontos científicos concedidos a §f" + target.getName() + "§a.");
                        openResearchPointTargets(p);
                    });
        }
        animateOpen(admin, holder);
    }

    void openRecipes(Player player) {
        if (!mechanium.hasAdminAccess(player)) {
            player.sendMessage("§cVocê não tem permissão para abrir o painel administrativo.");
            return;
        }
        openVanillaRecipes(player);
    }

    private void openVanillaRecipes(Player admin) {
        openVanillaRecipes(admin, 0);
    }

    private void openVanillaRecipes(Player admin, int requestedPage) {
        List<VanillaRecipeCatalog.Entry> recipes = VanillaRecipeCatalog.discover();
        int pageCount = Math.max(1, (recipes.size() + CONTENT_SLOTS.length - 1) / CONTENT_SLOTS.length);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        int start = page * CONTENT_SLOTS.length;

        AdminHolder holder = create("MECHANIUM • Receitas vanilla");
        Inventory inv = holder.getInventory();
        fillFrame(inv);
        boolean enabled = plugin.getConfig().getBoolean("vanilla-recipe-gates.enabled", true);
        inv.setItem(4, item(Material.CRAFTING_TABLE, "§bReceitas vanilla por era", List.of(
                "§7Receitas descobertas: §f" + recipes.size(),
                "§7Página: §f" + (page + 1) + "§7/§f" + pageCount,
                "§7Sistema global: " + (enabled ? "§aativado" : "§cdesativado"),
                "§7Clique num item para avançar sua era exigida.",
                "§7Depois da Era Mítica, o ciclo passa por Futuro Especulativo e sem bloqueio.",
                "§7Comando: /mechanium admin recipe <id> <era|off>"
        )));
        button(holder, 49, Material.ARROW, "§eVoltar", List.of(), this::openHome);

        int count = Math.min(CONTENT_SLOTS.length, recipes.size() - start);
        for (int i = 0; i < count; i++) {
            VanillaRecipeCatalog.Entry recipe = recipes.get(start + i);
            String configPath = "vanilla-recipe-gates.recipes." + recipe.key();
            String requiredEra = plugin.getConfig().getString(configPath);
            String state = requiredEra == null || requiredEra.equalsIgnoreCase("off") ? "§aSem bloqueio" :
                    api.getEra(requiredEra).map(era -> "§e" + era.displayName()).orElse("§cEra inválida: " + requiredEra);
            button(holder, CONTENT_SLOTS[i], recipe.result(), "§b" + recipe.displayName(),
                    List.of("§7Receita: §f" + recipe.key(),
                            "§7Tipo: §f" + recipe.type(),
                            "§7Requisito: " + state,
                            "§8Era inicial sugerida: §f" + api.getEra(recipe.suggestedEraId())
                                    .map(EraDefinition::displayName).orElse(recipe.suggestedEraId()),
                            "§aClique para avançar a era exigida."),
                    p -> {
                        cycleRecipeRequirement(configPath);
                        openVanillaRecipes(p, page);
                    });
        }

        if (page > 0) button(holder, 45, Material.PAPER, "§ePágina anterior",
                List.of("§7Página " + page + " de " + pageCount), p -> openVanillaRecipes(p, page - 1));
        if (page + 1 < pageCount) button(holder, 53, Material.PAPER, "§ePróxima página",
                List.of("§7Página " + (page + 2) + " de " + pageCount), p -> openVanillaRecipes(p, page + 1));

        animateOpen(admin, holder);
    }

    private void cycleRecipeRequirement(String path) {
        List<EraDefinition> eras = orderedEras();
        String currentId = plugin.getConfig().getString(path);
        int currentIndex = -1;
        for (int i = 0; i < eras.size(); i++) {
            if (eras.get(i).id().equalsIgnoreCase(Objects.toString(currentId, ""))) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = currentIndex + 1;
        if (nextIndex >= eras.size()) {
            plugin.getConfig().set(path, "off");
        } else {
            plugin.getConfig().set(path, eras.get(nextIndex).id());
        }
        plugin.saveConfig();
    }

    private List<EraDefinition> orderedEras() {
        return api.getEras().stream().sorted(Comparator.comparingInt(EraDefinition::order)).toList();
    }

    private String currentEraName(Player player) {
        return api.getPlayerEra(player.getUniqueId()).flatMap(api::getEra)
                .map(EraDefinition::displayName).orElse("Não definida");
    }

    private String defaultEraName() {
        String id = plugin.getConfig().getString("progression.default-era", "ancient");
        return api.getEra(id).map(EraDefinition::displayName).orElse(id);
    }

    private void animateOpen(Player player, AdminHolder holder) {
        holder.animating = true;
        MechaniumMenuAnimator.open(plugin, player, holder.inventory, () -> holder.animating = false);
    }

    private int configuredRecipeCount() {
        var section = plugin.getConfig().getConfigurationSection("vanilla-recipe-gates.recipes");
        if (section == null) return 0;
        return (int) section.getKeys(false).stream()
                .filter(key -> !plugin.getConfig().getString("vanilla-recipe-gates.recipes." + key, "off").equalsIgnoreCase("off"))
                .count();
    }

    private AdminHolder create(String title) {
        AdminHolder holder = new AdminHolder();
        holder.inventory = Bukkit.createInventory(holder, SIZE, Component.text(title));
        return holder;
    }

    private void fillFrame(Inventory inventory) {
        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < SIZE; slot++) {
            if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) inventory.setItem(slot, pane);
        }
    }

    private void button(AdminHolder holder, int slot, Material material, String name,
                        List<String> lore, Consumer<Player> action) {
        holder.inventory.setItem(slot, item(material, name, lore));
        holder.actions.put(slot, action);
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(LegacyComponentSerializer.legacySection()::deserialize).toList());
        }
        stack.setItemMeta(meta);
        return stack;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof AdminHolder holder)) return;
        event.setCancelled(true);
        if (!mechanium.hasAdminAccess(player)) {
            player.closeInventory();
            player.sendMessage("§cSeu acesso administrativo não está mais ativo.");
            return;
        }
        if (holder.animating) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        Consumer<Player> action = holder.actions.get(event.getSlot());
        if (action != null) action.accept(player);
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof AdminHolder) event.setCancelled(true);
    }

    private static final class AdminHolder implements InventoryHolder {
        private Inventory inventory;
        private boolean animating;
        private final Map<Integer, Consumer<Player>> actions = new HashMap<>();
        @Override public Inventory getInventory() { return inventory; }
    }
}
