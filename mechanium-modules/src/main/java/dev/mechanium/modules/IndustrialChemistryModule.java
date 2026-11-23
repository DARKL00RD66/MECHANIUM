package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Native MECHANIUM module: industrial-chemistry. */
final class IndustrialChemistryModule extends AbstractBuiltinModule {
    @Override public String id() { return "industrial-chemistry"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry", "geology"); }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("industrial_reagents", "Reagentes industriais", "industrial_revolution", 34, List.of()),
            new TechnologyDefinition("polymer_engineering", "Engenharia de polímeros", "modern", 50, List.of())
        );
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("industrial_chemistry_goods", "Química industrial", "Materiais abstratos de processamento e polímeros para progressão do jogo"));
        registerItem(api, new ItemDefinition("catalyst_powder", "&ePó catalisador", "GLOWSTONE_DUST", List.of("&8Conteúdo do módulo industrial-chemistry"), 0, 64, "industrial_chemistry_goods", "industrial_reagents"));
        registerItem(api, new ItemDefinition("polymer_resin", "&eResina polimérica", "SLIME_BALL", List.of("&8Conteúdo do módulo industrial-chemistry"), 0, 64, "industrial_chemistry_goods", "industrial_reagents"));
        registerItem(api, new ItemDefinition("synthetic_fiber", "&eFibra sintética", "STRING", List.of("&8Conteúdo do módulo industrial-chemistry"), 0, 64, "industrial_chemistry_goods", "polymer_engineering"));
        registerItem(api, new ItemDefinition("polymer_mixer", "&eMisturador de polímeros", "DISPENSER", List.of("&8Conteúdo do módulo industrial-chemistry"), 0, 64, "industrial_chemistry_goods", "polymer_engineering"));
        registerRecipe(api, new RecipeDefinition("catalyst_powder_recipe", RecipeDefinition.Type.SHAPELESS, "catalyst_powder", 2, List.of(), Map.of('G', "GLOWSTONE_DUST", 'Q', "QUARTZ"), "industrial_reagents"));
        registerRecipe(api, new RecipeDefinition("polymer_resin_recipe", RecipeDefinition.Type.SHAPELESS, "polymer_resin", 2, List.of(), Map.of('S', "SLIME_BALL", 'C', "CHARCOAL"), "industrial_reagents"));
        registerRecipe(api, new RecipeDefinition("synthetic_fiber_recipe", RecipeDefinition.Type.SHAPELESS, "synthetic_fiber", 4, List.of(), Map.of('R', "mechanium:polymer_resin", 'W', "WHITE_WOOL"), "polymer_engineering"));
        registerRecipe(api, new RecipeDefinition("polymer_mixer_recipe", RecipeDefinition.Type.SHAPED, "polymer_mixer", 1, List.of("III", "RDR", "III"), Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'D', "DISPENSER"), "polymer_engineering"));
        registerMachine(api, new MachineDefinition("polymer_mixer", "Misturador de polímeros", "polymer_mixer", "DISPENSER", "mechanium:polymer_resin", 1, "synthetic_fiber", 2, 1400, "polymer_engineering"));
    }
}
