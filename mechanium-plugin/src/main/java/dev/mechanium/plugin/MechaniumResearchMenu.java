package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import dev.mechanium.api.model.UnlockResult;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * In-game research catalogue. All unlock decisions are delegated to the central API.
 * The menu is only a presentation layer and cannot bypass progression rules.
 */
final class MechaniumResearchMenu implements Listener {
    private static final int INVENTORY_SIZE = 54;
    private static final int PAGE_CAPACITY = 28;
    private static final int[] TECHNOLOGY_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final JavaPlugin plugin;
    private final MechaniumApi api;
    private final NamespacedKey technologyKey;

    MechaniumResearchMenu(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
        this.technologyKey = new NamespacedKey(plugin, "research_technology");
    }

    void open(Player player) {
        open(player, 0);
    }

    private void open(Player player, int requestedPage) {
        List<TechnologyDefinition> technologies = orderedTechnologies();
        int pageCount = Math.max(1, (technologies.size() + PAGE_CAPACITY - 1) / PAGE_CAPACITY);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));

        MenuHolder holder = new MenuHolder(page);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE,
                color("&8MECHANIUM &7| &fPesquisas"));
        holder.attach(inventory);

        ItemStack border = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < 9; slot++) inventory.setItem(slot, border);
        for (int slot = 45; slot < 54; slot++) inventory.setItem(slot, border);
        for (int slot : new int[]{9, 17, 18, 26, 27, 35, 36, 44}) inventory.setItem(slot, border);

        int start = page * PAGE_CAPACITY;
        int end = Math.min(start + PAGE_CAPACITY, technologies.size());
        for (int index = start; index < end; index++) {
            TechnologyDefinition technology = technologies.get(index);
            inventory.setItem(TECHNOLOGY_SLOTS[index - start], technologyItem(player, technology));
        }

        if (technologies.isEmpty()) {
            inventory.setItem(22, item(Material.BOOK, "&eNenhuma pesquisa registrada",
                    List.of("&7Os módulos ainda não registraram tecnologias.")));
        }

        EraDefinition currentEra = api.getPlayerEra(player.getUniqueId())
                .flatMap(api::getEra).orElse(null);
        String eraName = currentEra == null ? "Não definida" : currentEra.displayName();
        inventory.setItem(49, item(Material.CLOCK, "&6Sua progressão", List.of(
                "&7Era atual: &f" + eraName,
                "&7Pontos científicos: &f" + api.getResearchPoints(player.getUniqueId()),
                "&7Página: &f" + (page + 1) + "/" + pageCount,
                "",
                "&eClique em uma tecnologia para pesquisar."
        )));

        if (page > 0) {
            inventory.setItem(45, item(Material.ARROW, "&ePágina anterior", List.of("&7Voltar uma página")));
        }
        if (page + 1 < pageCount) {
            inventory.setItem(53, item(Material.ARROW, "&ePróxima página", List.of("&7Avançar uma página")));
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize()) return;
        if (slot == 45 && holder.page > 0) {
            open(player, holder.page - 1);
            return;
        }
        if (slot == 53) {
            open(player, holder.page + 1);
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        String technologyId = clicked.getItemMeta().getPersistentDataContainer()
                .get(technologyKey, PersistentDataType.STRING);
        if (technologyId == null) return;

        UnlockResult result = api.unlockTechnology(player.getUniqueId(), technologyId);
        player.sendMessage((result.successful() ? ChatColor.GREEN : ChatColor.RED) + result.message());
        open(player, holder.page);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private ItemStack technologyItem(Player player, TechnologyDefinition technology) {
        boolean unlocked = api.hasUnlockedTechnology(player.getUniqueId(), technology.id());
        boolean usable = api.canUseTechnology(player.getUniqueId(), technology.id());
        EraDefinition requiredEra = api.getEra(technology.requiredEraId()).orElse(null);
        String eraName = requiredEra == null ? technology.requiredEraId() : requiredEra.displayName();

        List<String> lore = new ArrayList<>();
        lore.add(color("&7ID: &8" + technology.id()));
        lore.add(color("&7Era necessária: &f" + eraName));
        lore.add(color("&7Custo científico: &f" + technology.researchCost()));
        if (!technology.prerequisites().isEmpty()) {
            lore.add(color("&7Pré-requisitos:"));
            technology.prerequisites().forEach(id -> lore.add(color("&8• &7" + id)));
        }
        lore.add("");
        if (unlocked && usable) {
            lore.add(color("&aPesquisa concluída e disponível"));
        } else if (unlocked) {
            lore.add(color("&ePesquisa concluída"));
            lore.add(color("&cIndisponível na era atual"));
        } else {
            lore.add(color("&cAinda não pesquisada"));
            Optional<String> currentEraId = api.getPlayerEra(player.getUniqueId());
            EraDefinition currentEra = currentEraId.flatMap(api::getEra).orElse(null);
            if (currentEra != null && requiredEra != null && currentEra.order() < requiredEra.order()) {
                lore.add(color("&cExige uma era posterior"));
            } else if (technology.prerequisites().stream()
                    .anyMatch(id -> !api.hasUnlockedTechnology(player.getUniqueId(), id))) {
                lore.add(color("&cFaltam pesquisas anteriores"));
            } else if (api.getResearchPoints(player.getUniqueId()) < technology.researchCost()) {
                lore.add(color("&cPontos científicos insuficientes"));
            }
            lore.add(color("&eClique para tentar pesquisar"));
        }

        Material icon = unlocked ? Material.ENCHANTED_BOOK : Material.BOOK;
        ItemStack stack = item(icon, (unlocked ? "&a" : "&e") + technology.displayName(), lore);
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(technologyKey, PersistentDataType.STRING, technology.id());
        stack.setItemMeta(meta);
        return stack;
    }

    private List<TechnologyDefinition> orderedTechnologies() {
        return api.getTechnologies().stream()
                .sorted(Comparator
                        .comparingInt((TechnologyDefinition value) -> api.getEra(value.requiredEraId())
                                .map(EraDefinition::order).orElse(Integer.MAX_VALUE))
                        .thenComparing(TechnologyDefinition::id))
                .toList();
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(color(name));
        if (!lore.isEmpty()) meta.setLore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static String color(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

    private static final class MenuHolder implements InventoryHolder {
        private final int page;
        private Inventory inventory;

        private MenuHolder(int page) {
            this.page = page;
        }

        private void attach(Inventory value) {
            inventory = value;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
