package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Native, progression-gated material generators inspired by the SMG gameplay domain. */
final class MaterialGeneratorsModule extends AbstractBuiltinModule {
    @Override public String id() { return "material-generators"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("basic_material_generation", "Geração básica de materiais",
                        "industrial_revolution", 48, List.of("mechanized_production")),
                new TechnologyDefinition("advanced_material_generation", "Geração avançada de materiais",
                        "modern", 64, List.of("basic_material_generation", "industrial_efficiency"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("material_generators", "Geradores de materiais",
                "Máquinas passivas que produzem materiais em um baú posicionado acima delas"));
        registerGenerator(api, "material_generator_cobblestone", "Gerador de pedregulho", "COBBLESTONE",
                "basic_material_generation", "COBBLESTONE", 8);
        registerGenerator(api, "material_generator_stone", "Gerador de pedra", "STONE",
                "basic_material_generation", "STONE", 12);
        registerGenerator(api, "material_generator_gravel", "Gerador de cascalho", "GRAVEL",
                "basic_material_generation", "GRAVEL", 16);
        registerGenerator(api, "material_generator_sand", "Gerador de areia", "SAND",
                "basic_material_generation", "SAND", 16);
        registerGenerator(api, "material_generator_smooth_stone", "Gerador de pedra lisa", "SMOOTH_STONE",
                "advanced_material_generation", "SMOOTH_STONE", 20);
        registerGenerator(api, "material_generator_glass", "Gerador de vidro", "GLASS",
                "advanced_material_generation", "GLASS", 24);
        registerGenerator(api, "material_generator_netherrack", "Gerador de netherrack", "NETHERRACK",
                "advanced_material_generation", "NETHERRACK", 28);
        registerGenerator(api, "material_generator_soul_sand", "Gerador de areia das almas", "SOUL_SAND",
                "advanced_material_generation", "SOUL_SAND", 32);
    }

    private void registerGenerator(dev.mechanium.api.MechaniumApi api, String id, String name,
                                   String loreMaterial, String technology, String output, int cost) {
        registerItem(api, new ItemDefinition(id, "&e" + name, "DISPENSER",
                List.of("&8Gerador passivo de materiais", "&7Coloque um baú diretamente acima", "&7Produz 1 " + output.toLowerCase().replace('_', ' ') + " por ciclo"),
                0, 1, "material_generators", technology));
        registerRecipe(api, new RecipeDefinition(id + "_recipe", RecipeDefinition.Type.SHAPED,
                id, 1, List.of("IRI", "DCD", "IRI"),
                Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'D', "DROPPER", 'C', "CHEST"),
                technology));
    }
}
