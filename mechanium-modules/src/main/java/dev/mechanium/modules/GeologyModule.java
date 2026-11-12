package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Historical resource-surveying tools inspired by ore-prospecting addons. */
final class GeologyModule extends AbstractBuiltinModule {
    @Override public String id() { return "geology"; }

    @Override public Set<String> dependencies() { return Set.of("industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(new TechnologyDefinition(
                "geological_surveying", "Prospecção geológica", "renaissance", 22,
                List.of("basic_mechanics")));
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory(
                "geology", "Geologia e mineração", "Ferramentas de prospecção e conhecimento mineral"));

        registerItem(api, new ItemDefinition(
                "geological_survey_kit", "&6Kit de prospecção geológica", "COMPASS",
                List.of("&8Instrumento de levantamento mineral",
                        "&7Use em um bloco para analisar a região próxima"),
                0, 1, "geology", "geological_surveying"));

        registerRecipe(api, new RecipeDefinition(
                "geological_survey_kit_recipe", RecipeDefinition.Type.SHAPED,
                "geological_survey_kit", 1,
                List.of(" P ", "ICI", " P "),
                Map.of('P', "PAPER", 'I', "IRON_INGOT", 'C', "COMPASS"),
                "geological_surveying"));
    }
}
