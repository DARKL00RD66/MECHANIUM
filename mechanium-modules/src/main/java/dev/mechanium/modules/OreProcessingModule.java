package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** First ore-processing chain; regional deposits and world generation remain a later milestone. */
final class OreProcessingModule extends AbstractBuiltinModule {
    @Override public String id() { return "ore-processing"; }
    @Override public Set<String> dependencies() { return Set.of("geology", "industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("ore_sifting", "Peneiramento de minério", "renaissance", 18, List.of("geological_surveying")),
                new TechnologyDefinition("ore_crushing", "Britagem de minério", "industrial_revolution", 34, List.of("basic_mechanics")),
                new TechnologyDefinition("ore_refining", "Refino de minério", "industrial_revolution", 48, List.of("ore_crushing"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory(
                "ore_processing", "Beneficiamento mineral", "Etapas de separação, britagem e refino"));
        registerItem(api, new ItemDefinition("crushed_ore", "&7Minério britado", "GRAVEL",
                List.of("&8Material intermediário do beneficiamento mineral"), 0, 64, "ore_processing", "ore_sifting"));
        registerItem(api, new ItemDefinition("iron_concentrate", "&6Concentrado de ferro", "RAW_IRON",
                List.of("&8Minério separado de parte das impurezas"), 0, 64, "ore_processing", "ore_crushing"));
        registerItem(api, new ItemDefinition("refined_alloy", "&bLiga refinada", "IRON_INGOT",
                List.of("&8Liga metálica padronizada para processos industriais"), 0, 64, "ore_processing", "ore_refining"));

        registerRecipe(api, new RecipeDefinition("crushed_ore_recipe", RecipeDefinition.Type.SHAPELESS,
                "crushed_ore", 2, List.of(), Map.of('R', "RAW_IRON", 'G', "GRAVEL"), "ore_sifting"));
        registerRecipe(api, new RecipeDefinition("iron_concentrate_recipe", RecipeDefinition.Type.SHAPELESS,
                "iron_concentrate", 1, List.of(), Map.of('O', "mechanium:crushed_ore", 'C', "COAL"), "ore_crushing"));
        registerRecipe(api, new RecipeDefinition("refined_alloy_recipe", RecipeDefinition.Type.SHAPELESS,
                "refined_alloy", 2, List.of(), Map.of('I', "mechanium:iron_concentrate", 'C', "COPPER_INGOT"),
                "ore_refining"));
    }
}
