package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Finite, recipe-costed material recovery; no free or infinite resource transmutation. */
final class EMCTechModule extends AbstractBuiltinModule {
    @Override public String id() { return "emc-tech"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("ore-processing", "environment"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("material_value_analysis", "Análise de materiais",
                        "modern", 52, List.of("ore_refining", "circular_industry")),
                new TechnologyDefinition("precision_material_recovery", "Recuperação precisa de materiais",
                        "information_age", 68, List.of("material_value_analysis", "data_processing"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("material_recovery", "Recuperação de materiais",
                "Conversão controlada de insumos em materiais processados"));

        registerItem(api, new ItemDefinition("recovered_silicon", "&bSilício recuperado", "PAPER",
                List.of("&8Material processado a partir de quartzo e cobre"),
                0, 64, "material_recovery", "material_value_analysis"));
        registerItem(api, new ItemDefinition("composite_alloy", "&7Liga composta", "IRON_INGOT",
                List.of("&8Liga industrial recuperada a partir de insumos finitos"),
                0, 64, "material_recovery", "material_value_analysis"));
        registerItem(api, new ItemDefinition("precision_crystal", "&dCristal de precisão", "AMETHYST_SHARD",
                List.of("&8Componente mineral processado para tecnologias avançadas"),
                0, 32, "material_recovery", "precision_material_recovery"));

        registerRecipe(api, new RecipeDefinition("recovered_silicon_recipe", RecipeDefinition.Type.SHAPELESS,
                "recovered_silicon", 2, List.of(),
                Map.of('Q', "QUARTZ", 'C', "COPPER_INGOT", 'R', "REDSTONE"),
                "material_value_analysis"));
        registerRecipe(api, new RecipeDefinition("composite_alloy_recipe", RecipeDefinition.Type.SHAPELESS,
                "composite_alloy", 2, List.of(),
                Map.of('I', "IRON_INGOT", 'C', "COPPER_INGOT", 'N', "IRON_NUGGET"),
                "material_value_analysis"));
        registerRecipe(api, new RecipeDefinition("precision_crystal_recipe", RecipeDefinition.Type.SHAPELESS,
                "precision_crystal", 1, List.of(),
                Map.of('A', "AMETHYST_SHARD", 'S', "mechanium:recovered_silicon",
                        'G', "GOLD_INGOT"),
                "precision_material_recovery"));
    }
}
