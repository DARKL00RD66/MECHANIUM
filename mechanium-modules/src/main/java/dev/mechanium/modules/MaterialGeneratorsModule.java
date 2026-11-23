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
        registerGenerator(api, "material_generator_cobblestone", "Gerador de pedregulho", "basic_material_generation", "COBBLESTONE");
        registerGenerator(api, "material_generator_stone", "Gerador de pedra", "basic_material_generation", "STONE");
        registerGenerator(api, "material_generator_gravel", "Gerador de cascalho", "basic_material_generation", "GRAVEL");
        registerGenerator(api, "material_generator_sand", "Gerador de areia", "basic_material_generation", "SAND");
        registerGenerator(api, "material_generator_smooth_stone", "Gerador de pedra lisa", "advanced_material_generation", "SMOOTH_STONE");
        registerGenerator(api, "material_generator_glass", "Gerador de vidro", "advanced_material_generation", "GLASS");
        registerGenerator(api, "material_generator_netherrack", "Gerador de netherrack", "advanced_material_generation", "NETHERRACK");
        registerGenerator(api, "material_generator_soul_sand", "Gerador de areia das almas", "advanced_material_generation", "SOUL_SAND");
    }

    private void registerGenerator(dev.mechanium.api.MechaniumApi api, String id, String name,
                                   String technology, String output) {
        registerItem(api, new ItemDefinition(id, "&e" + name, "DISPENSER",
                List.of("&8Gerador passivo de materiais", "&7Coloque um baú diretamente acima", "&7Produz 1 " + output.toLowerCase().replace('_', ' ') + " por ciclo"),
                0, 1, "material_generators", technology));
        registerRecipe(api, new RecipeDefinition(id + "_recipe", RecipeDefinition.Type.SHAPED,
                id, 1, List.of("IRI", "DCD", "IMI"),
                Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'D', "DROPPER", 'C', "CHEST", 'M', output),
                technology));
    }
}
