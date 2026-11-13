package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;
import java.util.Map;

final class EnvironmentModule extends AbstractBuiltinModule {
    @Override public String id() { return "environment"; }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("waste_management", "Gestão de resíduos", "industrial_revolution", 20, List.of()),
            new TechnologyDefinition("emissions_control", "Controle de emissões", "modern", 45, List.of("waste_management")),
            new TechnologyDefinition("circular_industry", "Indústria circular", "modern", 55, List.of("emissions_control"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("environmental_goods", "Gestão ambiental",
                "Materiais para redução de resíduos e controle de emissões"));
        registerItem(api, new ItemDefinition("recycled_material", "&aMaterial reciclado", "PAPER",
                List.of("&8Matéria-prima recuperada de resíduos industriais"),
                0, 64, "environmental_goods", "waste_management"));
        registerItem(api, new ItemDefinition("carbon_filter", "&7Filtro de carbono", "HOPPER",
                List.of("&8Componente de controle de emissões", "&7Representa filtragem industrial abstrata"),
                0, 16, "environmental_goods", "emissions_control"));
        registerItem(api, new ItemDefinition("reclamation_kit", "&2Kit de recuperação", "COMPOSTER",
                List.of("&8Conjunto para recuperação e reaproveitamento de materiais"),
                0, 16, "environmental_goods", "circular_industry"));

        registerRecipe(api, new RecipeDefinition("recycled_material_recipe", RecipeDefinition.Type.SHAPELESS,
                "recycled_material", 2, List.of(), Map.of('I', "IRON_NUGGET", 'P', "PAPER", 'C', "CHARCOAL"),
                "waste_management"));
        registerRecipe(api, new RecipeDefinition("carbon_filter_recipe", RecipeDefinition.Type.SHAPED,
                "carbon_filter", 1, List.of("CCC", "CHC", "CCC"),
                Map.of('C', "CHARCOAL", 'H', "HOPPER"), "emissions_control"));
        registerRecipe(api, new RecipeDefinition("reclamation_kit_recipe", RecipeDefinition.Type.SHAPED,
                "reclamation_kit", 1, List.of("RCR", " S ", "RCR"),
                Map.of('R', "mechanium:recycled_material", 'C', "COMPOSTER", 'S', "SHEARS"),
                "circular_industry"));
    }
}
