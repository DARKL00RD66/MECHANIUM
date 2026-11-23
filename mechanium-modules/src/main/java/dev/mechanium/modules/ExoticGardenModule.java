package dev.mechanium.modules;

import dev.mechanium.api.MechaniumModuleContext;
import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * First independent horticulture expansion inspired by ExoticGarden:
 * additional botanical inputs, processed extracts and packaged harvests.
 */
final class ExoticGardenModule extends AbstractBuiltinModule {
    @Override public String id() { return "exotic-garden"; }

    @Override public Set<String> dependencies() { return Set.of("agriculture"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("botanical_gardening", "Botânica aplicada", "renaissance", 24,
                        List.of("crop_rotation")),
                new TechnologyDefinition("greenhouse_botany", "Botânica de estufa", "modern", 42,
                        List.of("greenhouse_cultivation", "botanical_gardening")),
                new TechnologyDefinition("exotic_horticulture", "Horticultura exótica", "information_age", 58,
                        List.of("greenhouse_botany"))
        );
    }

    @Override protected void registerExtra(MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory(
                "exotic_horticulture", "Horticultura exótica",
                "Sementes, ervas, extratos botânicos e colheitas especiais"));

        registerItem(api, new ItemDefinition("exotic_seed_mix", "&aMistura de sementes exóticas",
                "BEETROOT_SEEDS",
                List.of("&8Seleção preparada de sementes para cultivo diversificado"),
                0, 64, "exotic_horticulture", "botanical_gardening"));
        registerItem(api, new ItemDefinition("dried_herbs", "&2Ervas desidratadas",
                "DRIED_KELP",
                List.of("&8Ervas selecionadas e preparadas para processamento"),
                0, 64, "exotic_horticulture", "botanical_gardening"));
        registerItem(api, new ItemDefinition("herbal_extract", "&aExtrato botânico",
                "GLOW_INK_SAC",
                List.of("&8Concentrado vegetal para cadeias de produção avançadas"),
                0, 64, "exotic_horticulture", "greenhouse_botany"));
        registerItem(api, new ItemDefinition("tropical_preserve", "&6Conserva tropical",
                "HONEY_BOTTLE",
                List.of("&8Frutas selecionadas conservadas para armazenamento"),
                0, 16, "exotic_horticulture", "exotic_horticulture"));
        registerItem(api, new ItemDefinition("orchard_crate", "&6Caixote de pomar",
                "CHEST",
                List.of("&8Embalagem de colheitas diversificadas"),
                0, 16, "exotic_horticulture", "exotic_horticulture"));

        registerRecipe(api, new RecipeDefinition("exotic_seed_mix_recipe",
                RecipeDefinition.Type.SHAPELESS, "exotic_seed_mix", 4, List.of(),
                Map.of('B', "BEETROOT_SEEDS", 'C', "CARROT", 'P', "POTATO", 'W', "WHEAT_SEEDS"),
                "botanical_gardening"));
        registerRecipe(api, new RecipeDefinition("dried_herbs_recipe",
                RecipeDefinition.Type.SHAPELESS, "dried_herbs", 2, List.of(),
                Map.of('L', "JUNGLE_LEAVES", 'S', "SWEET_BERRIES", 'G', "GLOW_BERRIES"),
                "botanical_gardening"));
        registerRecipe(api, new RecipeDefinition("herbal_extract_recipe",
                RecipeDefinition.Type.SHAPELESS, "herbal_extract", 2, List.of(),
                Map.of('H', "mechanium:dried_herbs", 'B', "BONE_MEAL", 'G', "GLOW_INK_SAC"),
                "greenhouse_botany"));
        registerRecipe(api, new RecipeDefinition("tropical_preserve_recipe",
                RecipeDefinition.Type.SHAPELESS, "tropical_preserve", 1, List.of(),
                Map.of('M', "MELON_SLICE", 'S', "SWEET_BERRIES", 'G', "GLOW_BERRIES", 'H', "HONEY_BOTTLE"),
                "exotic_horticulture"));
        registerRecipe(api, new RecipeDefinition("orchard_crate_recipe",
                RecipeDefinition.Type.SHAPED, "orchard_crate", 1,
                List.of("PPP", "PFP", "PPP"),
                Map.of('P', "OAK_PLANKS", 'F', "mechanium:tropical_preserve"),
                "exotic_horticulture"));
    }
}
