package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Utility equipment and reinforced hand-tool recipes, independently implemented for MECHANIUM. */
final class ExtraGearModule extends AbstractBuiltinModule {
    @Override public String id() { return "extra-gear"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("reinforced_toolmaking", "Ferramentas reforçadas", "renaissance", 16, List.of("basic_mechanics")),
                new TechnologyDefinition("field_engineering", "Engenharia de campo", "industrial_revolution", 28, List.of("reinforced_toolmaking"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("extra_gear", "Equipamentos utilitários", "Ferramentas e componentes de trabalho"));
        registerItem(api, new ItemDefinition("reinforced_handle", "&6Cabo reforçado", "STICK",
                List.of("&8Cabo reforçado para ferramentas especiais"), 0, 64, "extra_gear", "reinforced_toolmaking"));
        registerItem(api, new ItemDefinition("surveying_compass", "&bBússola de levantamento", "COMPASS",
                List.of("&8Instrumento de orientação e levantamento territorial"), 0, 1, "extra_gear", "field_engineering"));
        registerItem(api, new ItemDefinition("reinforced_pickaxe", "&6Picareta reforçada", "IRON_PICKAXE",
                List.of("&8Ferramenta de mineração reforçada", "&7Concede aceleração breve ao concluir uma mineração"),
                0, 1, "extra_gear", "reinforced_toolmaking"));

        registerRecipe(api, new RecipeDefinition("reinforced_handle_recipe", RecipeDefinition.Type.SHAPED,
                "reinforced_handle", 2, List.of(" I ", " I ", " S "),
                Map.of('I', "IRON_INGOT", 'S', "STICK"), "reinforced_toolmaking"));
        registerRecipe(api, new RecipeDefinition("surveying_compass_recipe", RecipeDefinition.Type.SHAPED,
                "surveying_compass", 1, List.of(" P ", "IRI", " P "),
                Map.of('P', "PAPER", 'I', "IRON_INGOT", 'R', "REDSTONE"), "field_engineering"));
        registerRecipe(api, new RecipeDefinition("reinforced_pickaxe_recipe", RecipeDefinition.Type.SHAPED,
                "reinforced_pickaxe", 1, List.of("III", " H ", " H "),
                Map.of('I', "IRON_INGOT", 'H', "mechanium:reinforced_handle"), "reinforced_toolmaking"));
    }
}
