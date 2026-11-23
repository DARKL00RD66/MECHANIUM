package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Circular manufacturing chain; all outputs are in-game abstractions. */
final class IndustrialRecyclingModule extends AbstractBuiltinModule {
    @Override public String id() { return "industrial-recycling"; }
    @Override public Set<String> dependencies() { return Set.of("environment"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("mechanical_recycling", "Reciclagem mecânica", "industrial_revolution", 32, List.of("waste_management")),
            new TechnologyDefinition("precision_material_sorting", "Separação precisa de materiais", "modern", 50, List.of("mechanical_recycling", "emissions_control"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("industrial_recycling_goods", "Reciclagem industrial", "Sucata classificada e matéria-prima recuperada"));
        registerItem(api, new ItemDefinition("mixed_scrap", "&7Sucata mista", "IRON_NUGGET",
                List.of("&8Material recuperável de processos industriais"), 0, 64, "industrial_recycling_goods", "mechanical_recycling"));
        registerItem(api, new ItemDefinition("sorted_iron_scrap", "&fSucata ferrosa classificada", "RAW_IRON",
                List.of("&8Fração ferrosa separada"), 0, 64, "industrial_recycling_goods", "precision_material_sorting"));
        registerItem(api, new ItemDefinition("recycled_copper", "&6Cobre recuperado", "RAW_COPPER",
                List.of("&8Cobre recuperado de sucata selecionada"), 0, 64, "industrial_recycling_goods", "precision_material_sorting"));
        registerItem(api, new ItemDefinition("recycled_polymer", "&dPolímero recuperado", "SLIME_BALL",
                List.of("&8Representação abstrata de polímero reciclado"), 0, 64, "industrial_recycling_goods", "mechanical_recycling"));
        registerItem(api, new ItemDefinition("sorting_module", "&eMódulo de triagem", "COMPARATOR",
                List.of("&8Componente para linhas de separação de materiais"), 0, 16, "industrial_recycling_goods", "precision_material_sorting"));
        registerRecipe(api, new RecipeDefinition("mixed_scrap_recipe", RecipeDefinition.Type.SHAPELESS,
                "mixed_scrap", 4, List.of(), Map.of('I', "IRON_NUGGET", 'C', "COPPER_NUGGET", 'P', "PAPER"), "mechanical_recycling"));
        registerRecipe(api, new RecipeDefinition("sorted_iron_scrap_recipe", RecipeDefinition.Type.SHAPELESS,
                "sorted_iron_scrap", 2, List.of(), Map.of('S', "mechanium:mixed_scrap", 'G', "GRAVEL"), "precision_material_sorting"));
        registerRecipe(api, new RecipeDefinition("recycled_copper_recipe", RecipeDefinition.Type.SHAPELESS,
                "recycled_copper", 2, List.of(), Map.of('S', "mechanium:mixed_scrap", 'C', "COPPER_INGOT"), "precision_material_sorting"));
        registerRecipe(api, new RecipeDefinition("recycled_polymer_recipe", RecipeDefinition.Type.SHAPELESS,
                "recycled_polymer", 2, List.of(), Map.of('S', "mechanium:mixed_scrap", 'L', "SLIME_BALL"), "mechanical_recycling"));
        registerRecipe(api, new RecipeDefinition("sorting_module_recipe", RecipeDefinition.Type.SHAPED,
                "sorting_module", 1, List.of("IRI", "CHC", "IRI"),
                Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'C', "mechanium:mixed_scrap", 'H', "HOPPER"), "precision_material_sorting"));
    }
}
