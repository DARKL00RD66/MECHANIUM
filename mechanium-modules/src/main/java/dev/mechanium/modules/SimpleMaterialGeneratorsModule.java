package dev.mechanium.modules;

import dev.mechanium.api.model.ContentCategory;
import dev.mechanium.api.model.ItemDefinition;
import dev.mechanium.api.model.MachineDefinition;
import dev.mechanium.api.model.RecipeDefinition;
import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Simple powered-by-time material processing inspired by SimpleMaterialGenerators. */
final class SimpleMaterialGeneratorsModule extends AbstractBuiltinModule {
    @Override public String id() { return "simple-material-generators"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry", "geology"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(new TechnologyDefinition("industrial_sieving", "Peneiramento industrial",
                "industrial_revolution", 36, List.of("industrial_chemistry")));
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("material_generators", "Beneficiamento simples",
                "Máquinas de separação e processamento inicial de materiais"));
        registerItem(api, new ItemDefinition("gravel_sifter", "&6Peneira de cascalho", "DISPENSER",
                List.of("&8Máquina de separação mineral", "&7Entrada: 3 cascalhos", "&7Saída: 1 fragmento mineral"),
                0, 1, "material_generators", "industrial_sieving"));
        registerItem(api, new ItemDefinition("sand_separator", "&eSeparador de areia", "DISPENSER",
                List.of("&8Máquina de preparação de argila", "&7Entrada: 3 areias", "&7Saída: 1 pelota de argila"),
                0, 1, "material_generators", "industrial_sieving"));
        registerItem(api, new ItemDefinition("mineral_fragment", "&7Fragmento mineral", "FLINT",
                List.of("&8Material separado por peneiramento"),
                0, 64, "material_generators", "industrial_sieving"));
        registerItem(api, new ItemDefinition("clay_pellet", "&6Pelota de argila", "CLAY_BALL",
                List.of("&8Material preparado para processamento"),
                0, 64, "material_generators", "industrial_sieving"));

        registerRecipe(api, new RecipeDefinition("gravel_sifter_recipe", RecipeDefinition.Type.SHAPED,
                "gravel_sifter", 1, List.of("ISI", "DHD", "III"),
                Map.of('I', "IRON_INGOT", 'S', "STRING", 'D', "IRON_NUGGET", 'H', "HOPPER"),
                "industrial_sieving"));
        registerRecipe(api, new RecipeDefinition("sand_separator_recipe", RecipeDefinition.Type.SHAPED,
                "sand_separator", 1, List.of("IGI", "DHD", "III"),
                Map.of('I', "IRON_INGOT", 'G', "GLASS", 'D', "IRON_NUGGET", 'H', "HOPPER"),
                "industrial_sieving"));
        registerMachine(api, new MachineDefinition("gravel_sifter", "Peneira de cascalho",
                "gravel_sifter", "DISPENSER", "GRAVEL", 3,
                "mineral_fragment", 1, 1200, "industrial_sieving"));
        registerMachine(api, new MachineDefinition("sand_separator", "Separador de areia",
                "sand_separator", "DISPENSER", "SAND", 3,
                "clay_pellet", 1, 1200, "industrial_sieving"));
    }
}
