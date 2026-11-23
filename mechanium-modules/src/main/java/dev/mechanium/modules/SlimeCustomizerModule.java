package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Blueprint materials supporting MECHANIUM's own recipe customization tools. */
final class SlimeCustomizerModule extends AbstractBuiltinModule {
    @Override public String id() { return "slime-customizer"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industrial-automation"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(new TechnologyDefinition("recipe_engineering", "Engenharia de receitas",
                "information_age", 62, List.of("industrial_automation")));
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("recipe_customization", "Projetos e receitas",
                "Materiais de projeto para ferramentas administrativas de conteúdo"));
        registerItem(api, new ItemDefinition("recipe_blueprint", "&bProjeto de receita", "PAPER",
                List.of("&8Documento técnico", "&7Representa o projeto de uma receita personalizada"),
                0, 16, "recipe_customization", "recipe_engineering"));
        registerItem(api, new ItemDefinition("machine_blueprint", "&dProjeto de máquina", "BOOK",
                List.of("&8Projeto de equipamento industrial", "&7Usado como componente de projetos avançados"),
                0, 16, "recipe_customization", "recipe_engineering"));
        registerRecipe(api, new RecipeDefinition("recipe_blueprint_recipe", RecipeDefinition.Type.SHAPELESS,
                "recipe_blueprint", 2, List.of(),
                Map.of('P', "PAPER", 'I', "INK_SAC", 'R', "REDSTONE", 'C', "COPPER_INGOT"),
                "recipe_engineering"));
        registerRecipe(api, new RecipeDefinition("machine_blueprint_recipe", RecipeDefinition.Type.SHAPELESS,
                "machine_blueprint", 1, List.of(),
                Map.of('B', "mechanium:recipe_blueprint", 'C', "COMPARATOR", 'I', "IRON_INGOT"),
                "recipe_engineering"));
    }
}
