package dev.mechanium.plugin;

import dev.mechanium.api.MechaniumApi;
import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.EraDefinition;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.MachineDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import dev.mechanium.api.model.UnlockResult;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Native, non-inventory MECHANIUM interface.
 *
 * The interface is sent by Paper's server-side Dialog API. Players do not install
 * a client mod, resource pack, or separate launcher. The vanilla client must
 * support Minecraft dialogs (Java Edition 1.21.6+).
 */
@SuppressWarnings("UnstableApiUsage")
final class MechaniumResearchMenu implements Listener {
    private static final String NAMESPACE = "mechanium";
    private static final int PAGE_SIZE = 8;
    private static final int BUTTON_WIDTH = 220;

    private final JavaPlugin plugin;
    private final MechaniumApi api;

    MechaniumResearchMenu(JavaPlugin plugin, MechaniumApi api) {
        this.plugin = plugin;
        this.api = api;
    }

    void open(Player player) {
        openHome(player);
    }

    private void openHome(Player player) {
        String eraName = currentEra(player).map(EraDefinition::displayName).orElse("Não definida");
        long unlocked = api.getTechnologies().stream()
                .filter(technology -> api.hasUnlockedTechnology(player.getUniqueId(), technology.id()))
                .count();

        Component summary = Component.text()
                .append(Component.text("Seu progresso tecnológico\n", NamedTextColor.GRAY))
                .append(Component.text("Era atual: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(eraName + "\n", NamedTextColor.GOLD))
                .append(Component.text("Pontos científicos: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(api.getResearchPoints(player.getUniqueId()) + "\n", NamedTextColor.AQUA))
                .append(Component.text("Pesquisas concluídas: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(unlocked + "/" + api.getTechnologies().size() + "\n\n", NamedTextColor.GREEN))
                .append(Component.text("Escolha uma seção para explorar o conteúdo do MECHANIUM.",
                        NamedTextColor.GRAY))
                .build();

        List<ActionButton> buttons = List.of(
                button("Árvore tecnológica", "Pesquise tecnologias e avance pelas eras.", "ui_research_page_0"),
                button("Catálogo de itens", "Consulte os itens registrados pelos módulos e addons.", "ui_items_page_0"),
                button("Livro de receitas", "Veja receitas e requisitos de fabricação.", "ui_recipes_page_0"),
                button("Máquinas", "Consulte máquinas, entradas, saídas e tecnologias necessárias.", "ui_machines_page_0"),
                button("Eras históricas", "Explore a sequência de eras do servidor.", "ui_eras_page_0")
        );
        show(player, "MECHANIUM | Central tecnológica", summary, buttons, null);
    }

    private void openResearch(Player player, int requestedPage) {
        List<TechnologyDefinition> entries = api.getTechnologies().stream()
                .sorted(Comparator
                        .comparingInt((TechnologyDefinition value) -> api.getEra(value.requiredEraId())
                                .map(EraDefinition::order).orElse(Integer.MAX_VALUE))
                        .thenComparing(TechnologyDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int page = clampPage(requestedPage, entries.size());
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());

        List<ActionButton> buttons = new ArrayList<>();
        for (int index = start; index < end; index++) {
            TechnologyDefinition technology = entries.get(index);
            boolean unlocked = api.hasUnlockedTechnology(player.getUniqueId(), technology.id());
            String status = unlocked ? "✓ " : "◇ ";
            String era = api.getEra(technology.requiredEraId())
                    .map(EraDefinition::displayName).orElse(technology.requiredEraId());
            String label = status + technology.displayName();
            String tooltip = "Era: " + era + " | Custo: " + technology.researchCost()
                    + (unlocked ? " | Concluída" : " | Clique para pesquisar");
            buttons.add(button(label, tooltip, "ui_unlock_" + technology.id()));
        }
        addPageButtons(buttons, "research", page, entries.size());

        Component body = Component.text()
                .append(Component.text("PROGRESSÃO CIENTÍFICA\n", NamedTextColor.GOLD))
                .append(Component.text("Era atual: " +
                        currentEra(player).map(EraDefinition::displayName).orElse("Não definida") + "\n",
                        NamedTextColor.GRAY))
                .append(Component.text("Pontos disponíveis: " +
                        api.getResearchPoints(player.getUniqueId()) + "\n", NamedTextColor.AQUA))
                .append(Component.text("Selecione uma tecnologia para tentar pesquisá-la. O servidor validará "
                        + "era, pré-requisitos e custo antes de liberar o conteúdo.", NamedTextColor.DARK_GRAY))
                .build();

        if (entries.isEmpty()) {
            buttons.add(button("Voltar ao início", "Retornar à central.", "ui_home"));
        }
        show(player, "MECHANIUM | Árvore tecnológica", body, buttons, homeButton());
    }

    private void openItems(Player player, int requestedPage) {
        List<ItemDefinition> entries = api.getItems().stream()
                .sorted(Comparator.comparing(ItemDefinition::categoryId)
                        .thenComparing(ItemDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int page = clampPage(requestedPage, entries.size());
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());

        List<ActionButton> buttons = new ArrayList<>();
        for (int index = start; index < end; index++) {
            ItemDefinition item = entries.get(index);
            boolean usable = api.canUseItem(player.getUniqueId(), item.id());
            String category = api.getCategory(item.categoryId())
                    .map(ContentCategory::displayName).orElse(item.categoryId());
            buttons.add(button((usable ? "✓ " : "◇ ") + item.displayName(),
                    "Categoria: " + category + (usable ? " | Disponível" : " | Requer progressão"),
                    "ui_item_detail_" + item.id() + "_" + page));
        }
        addPageButtons(buttons, "items", page, entries.size());
        if (entries.isEmpty()) {
            buttons.add(button("Voltar ao início", "Retornar à central.", "ui_home"));
        }

        Component body = Component.text()
                .append(Component.text("CATÁLOGO DE CONTEÚDO\n", NamedTextColor.GOLD))
                .append(Component.text("Os itens são apresentados por categoria. Abra uma entrada para ver "
                        + "material, descrição e tecnologia necessária.\n", NamedTextColor.GRAY))
                .append(Component.text("Itens registrados: " + entries.size(), NamedTextColor.DARK_GRAY))
                .build();
        show(player, "MECHANIUM | Catálogo de itens", body, buttons, homeButton());
    }

    private void openItemDetail(Player player, String itemId, int returnPage) {
        Optional<ItemDefinition> found = api.getItem(itemId);
        if (found.isEmpty()) {
            player.sendMessage(Component.text("Este item não está mais registrado.", NamedTextColor.RED));
            openItems(player, returnPage);
            return;
        }
        ItemDefinition item = found.get();
        String category = api.getCategory(item.categoryId())
                .map(ContentCategory::displayName).orElse(item.categoryId());
        String technology = item.requiredTechnologyId() == null ? "Nenhuma"
                : api.getTechnology(item.requiredTechnologyId())
                    .map(TechnologyDefinition::displayName).orElse(item.requiredTechnologyId());
        boolean usable = api.canUseItem(player.getUniqueId(), item.id());

        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.item(new ItemStack(Material.matchMaterial(item.material()) == null
                ? Material.BARRIER : Material.matchMaterial(item.material()))).build());
        body.add(DialogBody.plainMessage(Component.text()
                .append(Component.text("Categoria: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(category + "\n", NamedTextColor.GRAY))
                .append(Component.text("Material: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(item.material() + "\n", NamedTextColor.WHITE))
                .append(Component.text("Tecnologia necessária: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(technology + "\n", NamedTextColor.AQUA))
                .append(Component.text("Estado: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(usable ? "Disponível" : "Bloqueado pela progressão",
                        usable ? NamedTextColor.GREEN : NamedTextColor.RED))
                .build()));

        if (!item.lore().isEmpty()) {
            body.add(DialogBody.plainMessage(Component.text(String.join("\n", item.lore()),
                    NamedTextColor.GRAY)));
        }

        showWithBody(player, "MECHANIUM | " + item.displayName(), body,
                List.of(button("Voltar ao catálogo", "Retornar aos itens.", "ui_items_page_" + returnPage)),
                homeButton());
    }

    private void openRecipes(Player player, int requestedPage) {
        List<RecipeDefinition> entries = api.getRecipes().stream()
                .sorted(Comparator.comparing(RecipeDefinition::id))
                .toList();
        int page = clampPage(requestedPage, entries.size());
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());

        List<ActionButton> buttons = new ArrayList<>();
        for (int index = start; index < end; index++) {
            RecipeDefinition recipe = entries.get(index);
            boolean available = api.canCraft(player.getUniqueId(), recipe.id());
            String resultName = api.getItem(recipe.resultItemId())
                    .map(ItemDefinition::displayName).orElse(recipe.resultItemId());
            buttons.add(button((available ? "✓ " : "◇ ") + resultName,
                    "Receita: " + recipe.id() + (available ? " | Liberada" : " | Bloqueada"),
                    "ui_recipe_detail_" + recipe.id() + "_" + page));
        }
        addPageButtons(buttons, "recipes", page, entries.size());
        if (entries.isEmpty()) buttons.add(button("Voltar ao início", "Retornar à central.", "ui_home"));

        Component body = Component.text()
                .append(Component.text("RECEITAS DE FABRICAÇÃO\n", NamedTextColor.GOLD))
                .append(Component.text("Consulte o resultado, ingredientes e tecnologia exigida. "
                        + "A disponibilidade é calculada pela progressão do jogador.\n", NamedTextColor.GRAY))
                .append(Component.text("Receitas registradas: " + entries.size(), NamedTextColor.DARK_GRAY))
                .build();
        show(player, "MECHANIUM | Livro de receitas", body, buttons, homeButton());
    }

    private void openRecipeDetail(Player player, String recipeId, int returnPage) {
        Optional<RecipeDefinition> found = api.getRecipe(recipeId);
        if (found.isEmpty()) {
            player.sendMessage(Component.text("Esta receita não está mais registrada.", NamedTextColor.RED));
            openRecipes(player, returnPage);
            return;
        }
        RecipeDefinition recipe = found.get();
        String resultName = api.getItem(recipe.resultItemId())
                .map(ItemDefinition::displayName).orElse(recipe.resultItemId());
        String technology = recipe.requiredTechnologyId() == null ? "Nenhuma"
                : api.getTechnology(recipe.requiredTechnologyId())
                    .map(TechnologyDefinition::displayName).orElse(recipe.requiredTechnologyId());
        List<String> ingredients = new ArrayList<>();
        recipe.ingredients().forEach((symbol, ingredient) ->
                ingredients.add(symbol + " = " + ingredient));
        String mode = recipe.type() == RecipeDefinition.Type.SHAPED ? "Modelada" : "Sem forma";

        Component details = Component.text()
                .append(Component.text("Resultado: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(resultName + " x" + recipe.resultAmount() + "\n", NamedTextColor.GREEN))
                .append(Component.text("Tipo: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(mode + "\n", NamedTextColor.WHITE))
                .append(Component.text("Ingredientes:\n", NamedTextColor.DARK_GRAY))
                .append(Component.text(ingredients.isEmpty() ? "Não especificados" :
                        String.join("\n", ingredients) + "\n", NamedTextColor.GRAY))
                .append(Component.text("Tecnologia necessária: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(technology + "\n", NamedTextColor.AQUA))
                .append(Component.text("Estado: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(api.canCraft(player.getUniqueId(), recipe.id()) ? "Disponível" : "Bloqueada",
                        api.canCraft(player.getUniqueId(), recipe.id()) ? NamedTextColor.GREEN : NamedTextColor.RED))
                .build();

        show(player, "MECHANIUM | Receita " + recipe.id(), details,
                List.of(button("Voltar às receitas", "Retornar ao livro.", "ui_recipes_page_" + returnPage)),
                homeButton());
    }

    private void openMachines(Player player, int requestedPage) {
        List<MachineDefinition> entries = api.getMachines().stream()
                .sorted(Comparator.comparing(MachineDefinition::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int page = clampPage(requestedPage, entries.size());
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());

        List<ActionButton> buttons = new ArrayList<>();
        for (int index = start; index < end; index++) {
            MachineDefinition machine = entries.get(index);
            boolean available = api.canUseTechnology(player.getUniqueId(), machine.requiredTechnologyId());
            buttons.add(button((available ? "✓ " : "◇ ") + machine.displayName(),
                    "Entrada: " + machine.inputId() + " | Saída: " + machine.outputItemId(),
                    "ui_machine_detail_" + machine.id() + "_" + page));
        }
        addPageButtons(buttons, "machines", page, entries.size());
        if (entries.isEmpty()) buttons.add(button("Voltar ao início", "Retornar à central.", "ui_home"));

        Component body = Component.text()
                .append(Component.text("ENCICLOPÉDIA DE MÁQUINAS\n", NamedTextColor.GOLD))
                .append(Component.text("Máquinas registradas: " + entries.size() + "\n", NamedTextColor.GRAY))
                .append(Component.text("Selecione uma máquina para consultar seu funcionamento e requisitos.",
                        NamedTextColor.DARK_GRAY))
                .build();
        show(player, "MECHANIUM | Máquinas", body, buttons, homeButton());
    }

    private void openMachineDetail(Player player, String machineId, int returnPage) {
        Optional<MachineDefinition> found = api.getMachine(machineId);
        if (found.isEmpty()) {
            player.sendMessage(Component.text("Esta máquina não está mais registrada.", NamedTextColor.RED));
            openMachines(player, returnPage);
            return;
        }
        MachineDefinition machine = found.get();
        String input = displayContentName(machine.inputId());
        String output = api.getItem(machine.outputItemId())
                .map(ItemDefinition::displayName).orElse(machine.outputItemId());
        String techName = api.getTechnology(machine.requiredTechnologyId())
                .map(TechnologyDefinition::displayName).orElse(machine.requiredTechnologyId());
        boolean available = api.canUseTechnology(player.getUniqueId(), machine.requiredTechnologyId());

        Component details = Component.text()
                .append(Component.text("Bloco-base: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(machine.blockMaterial() + "\n", NamedTextColor.WHITE))
                .append(Component.text("Entrada: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(input + " x" + machine.inputAmount() + "\n", NamedTextColor.YELLOW))
                .append(Component.text("Saída: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(output + " x" + machine.outputAmount() + "\n", NamedTextColor.GREEN))
                .append(Component.text("Tempo: ", NamedTextColor.DARK_GRAY))
                .append(Component.text((machine.processingTicks() / 20.0) + " s\n", NamedTextColor.WHITE))
                .append(Component.text("Tecnologia necessária: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(techName + "\n", NamedTextColor.AQUA))
                .append(Component.text("Estado: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(available ? "Disponível" : "Bloqueada",
                        available ? NamedTextColor.GREEN : NamedTextColor.RED))
                .build();

        show(player, "MECHANIUM | " + machine.displayName(), details,
                List.of(button("Voltar às máquinas", "Retornar à enciclopédia.", "ui_machines_page_" + returnPage)),
                homeButton());
    }

    private void openEras(Player player, int requestedPage) {
        List<EraDefinition> entries = api.getEras().stream()
                .sorted(Comparator.comparingInt(EraDefinition::order))
                .toList();
        int page = clampPage(requestedPage, entries.size());
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());
        String currentId = api.getPlayerEra(player.getUniqueId()).orElse("");

        List<ActionButton> buttons = new ArrayList<>();
        for (int index = start; index < end; index++) {
            EraDefinition era = entries.get(index);
            buttons.add(button((era.id().equals(currentId) ? "★ " : "") + era.displayName(),
                    "Ordem histórica: " + era.order(),
                    "ui_era_detail_" + era.id() + "_" + page));
        }
        addPageButtons(buttons, "eras", page, entries.size());
        if (entries.isEmpty()) buttons.add(button("Voltar ao início", "Retornar à central.", "ui_home"));

        Component body = Component.text()
                .append(Component.text("LINHA DO TEMPO HISTÓRICA\n", NamedTextColor.GOLD))
                .append(Component.text("A estrela marca sua era atual. A evolução depende das regras "
                        + "de progressão configuradas pelo servidor.\n", NamedTextColor.GRAY))
                .append(Component.text("Eras registradas: " + entries.size(), NamedTextColor.DARK_GRAY))
                .build();
        show(player, "MECHANIUM | Eras históricas", body, buttons, homeButton());
    }

    private void openEraDetail(Player player, String eraId, int returnPage) {
        Optional<EraDefinition> found = api.getEra(eraId);
        if (found.isEmpty()) {
            player.sendMessage(Component.text("Esta era não está mais registrada.", NamedTextColor.RED));
            openEras(player, returnPage);
            return;
        }
        EraDefinition era = found.get();
        boolean current = api.getPlayerEra(player.getUniqueId()).filter(eraId::equals).isPresent();
        long technologies = api.getTechnologies().stream()
                .filter(technology -> technology.requiredEraId().equals(era.id())).count();

        Component details = Component.text()
                .append(Component.text("Posição na linha do tempo: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(String.valueOf(era.order()) + "\n", NamedTextColor.WHITE))
                .append(Component.text("Tecnologias nesta era: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(technologies + "\n", NamedTextColor.AQUA))
                .append(Component.text("Sua era atual: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(current ? "Sim" : "Não",
                        current ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .build();

        show(player, "MECHANIUM | " + era.displayName(), details,
                List.of(button("Voltar às eras", "Retornar à linha do tempo.", "ui_eras_page_" + returnPage)),
                homeButton());
    }

    @EventHandler
    public void onDialogClick(PlayerCustomClickEvent event) {
        if (!NAMESPACE.equals(event.getIdentifier().namespace())) return;
        if (!(event.getCommonConnection() instanceof PlayerGameConnection connection)) return;

        Player player = connection.getPlayer();
        String action = event.getIdentifier().value();

        if (action.equals("ui_home")) {
            openHome(player);
        } else if (action.equals("ui_research")) {
            openResearch(player, 0);
        } else if (action.equals("ui_items")) {
            openItems(player, 0);
        } else if (action.equals("ui_recipes")) {
            openRecipes(player, 0);
        } else if (action.equals("ui_machines")) {
            openMachines(player, 0);
        } else if (action.equals("ui_eras")) {
            openEras(player, 0);
        } else if (action.startsWith("ui_research_page_")) {
            openResearch(player, parsePage(action, "ui_research_page_"));
        } else if (action.startsWith("ui_items_page_")) {
            openItems(player, parsePage(action, "ui_items_page_"));
        } else if (action.startsWith("ui_recipes_page_")) {
            openRecipes(player, parsePage(action, "ui_recipes_page_"));
        } else if (action.startsWith("ui_machines_page_")) {
            openMachines(player, parsePage(action, "ui_machines_page_"));
        } else if (action.startsWith("ui_eras_page_")) {
            openEras(player, parsePage(action, "ui_eras_page_"));
        } else if (action.startsWith("ui_unlock_")) {
            String technologyId = action.substring("ui_unlock_".length());
            UnlockResult result = api.unlockTechnology(player.getUniqueId(), technologyId);
            player.sendMessage(Component.text(result.message(),
                    result.successful() ? NamedTextColor.GREEN : NamedTextColor.RED));
            openResearch(player, 0);
        } else if (action.startsWith("ui_item_detail_")) {
            String[] parts = splitDetailAction(action, "ui_item_detail_");
            if (parts != null) openItemDetail(player, parts[0], Integer.parseInt(parts[1]));
        } else if (action.startsWith("ui_recipe_detail_")) {
            String[] parts = splitDetailAction(action, "ui_recipe_detail_");
            if (parts != null) openRecipeDetail(player, parts[0], Integer.parseInt(parts[1]));
        } else if (action.startsWith("ui_machine_detail_")) {
            String[] parts = splitDetailAction(action, "ui_machine_detail_");
            if (parts != null) openMachineDetail(player, parts[0], Integer.parseInt(parts[1]));
        } else if (action.startsWith("ui_era_detail_")) {
            String[] parts = splitDetailAction(action, "ui_era_detail_");
            if (parts != null) openEraDetail(player, parts[0], Integer.parseInt(parts[1]));
        }
    }

    private void addPageButtons(List<ActionButton> buttons, String section, int page, int total) {
        int pageCount = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        if (page > 0) {
            buttons.add(button("← Página anterior", "Voltar uma página.",
                    "ui_" + section + "_page_" + (page - 1)));
        }
        if (page + 1 < pageCount) {
            buttons.add(button("Próxima página →", "Avançar uma página.",
                    "ui_" + section + "_page_" + (page + 1)));
        }
    }

    private void show(Player player, String title, Component body,
                      List<ActionButton> buttons, ActionButton exitAction) {
        showWithBody(player, title, List.of(DialogBody.plainMessage(body, 380)), buttons, exitAction);
    }

    private void showWithBody(Player player, String title, List<DialogBody> body,
                              List<ActionButton> buttons, ActionButton exitAction) {
        List<ActionButton> dialogButtons = buttons.isEmpty()
                ? List.of(button("Voltar ao início", "Retornar à central.", "ui_home"))
                : List.copyOf(buttons);
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(title, NamedTextColor.GOLD))
                        .canCloseWithEscape(true)
                        .body(body)
                        .build())
                .type(DialogType.multiAction(dialogButtons, exitAction, 2)));
        player.showDialog(dialog);
    }

    private ActionButton homeButton() {
        return button("⌂ Início", "Voltar à central tecnológica.", "ui_home");
    }

    private ActionButton button(String label, String tooltip, String action) {
        return ActionButton.builder(Component.text(label, NamedTextColor.WHITE))
                .tooltip(Component.text(tooltip, NamedTextColor.GRAY))
                .width(BUTTON_WIDTH)
                .action(DialogAction.customClick(Key.key(NAMESPACE, action), null))
                .build();
    }

    private Optional<EraDefinition> currentEra(Player player) {
        return api.getPlayerEra(player.getUniqueId()).flatMap(api::getEra);
    }

    private int clampPage(int requestedPage, int total) {
        int pageCount = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        return Math.max(0, Math.min(requestedPage, pageCount - 1));
    }

    private int parsePage(String action, String prefix) {
        try {
            return Math.max(0, Integer.parseInt(action.substring(prefix.length())));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String[] splitDetailAction(String action, String prefix) {
        String value = action.substring(prefix.length());
        int separator = value.lastIndexOf('_');
        if (separator <= 0 || separator == value.length() - 1) return null;
        try {
            int page = Integer.parseInt(value.substring(separator + 1));
            return new String[]{value.substring(0, separator), String.valueOf(Math.max(0, page))};
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String displayContentName(String inputId) {
        if (inputId.regionMatches(true, 0, "mechanium:", 0, 10)) {
            String id = inputId.substring(10);
            return api.getItem(id).map(ItemDefinition::displayName).orElse(id);
        }
        Material material = Material.matchMaterial(inputId);
        return material == null ? inputId : material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
