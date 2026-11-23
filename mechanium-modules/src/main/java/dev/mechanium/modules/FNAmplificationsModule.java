package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Native MECHANIUM module: fn-amplifications. */
final class FNAmplificationsModule extends AbstractBuiltinModule {
    @Override public String id() { return "fn-amplifications"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry", "energy"); }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("fn_amplification_basics", "Fundamentos de amplificação", "industrial_revolution", 34, List.of()),
            new TechnologyDefinition("fn_amplification_tuning", "Calibração de amplificadores", "modern", 48, List.of())
        );
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("fn_amplification_parts", "Amplificações industriais", "Núcleos e componentes fictícios para aprimorar máquinas MECHANIUM"));
        registerItem(api, new ItemDefinition("amplifier_core", "&eNúcleo amplificador", "AMETHYST_SHARD", List.of("&8Conteúdo do módulo fn-amplifications"), 0, 64, "fn_amplification_parts", "fn_amplification_basics"));
        registerItem(api, new ItemDefinition("reinforced_coil", "&eBobina reforçada", "COPPER_INGOT", List.of("&8Conteúdo do módulo fn-amplifications"), 0, 64, "fn_amplification_parts", "fn_amplification_basics"));
        registerItem(api, new ItemDefinition("amplification_module", "&eMódulo de amplificação", "COMPARATOR", List.of("&8Conteúdo do módulo fn-amplifications"), 0, 64, "fn_amplification_parts", "fn_amplification_tuning"));
        registerItem(api, new ItemDefinition("amplification_station", "&eEstação de amplificação", "DISPENSER", List.of("&8Conteúdo do módulo fn-amplifications"), 0, 64, "fn_amplification_parts", "fn_amplification_tuning"));
        registerRecipe(api, new RecipeDefinition("amplifier_core_recipe", RecipeDefinition.Type.SHAPELESS, "amplifier_core", 2, List.of(), Map.of('A', "AMETHYST_SHARD", 'R', "REDSTONE"), "fn_amplification_basics"));
        registerRecipe(api, new RecipeDefinition("reinforced_coil_recipe", RecipeDefinition.Type.SHAPELESS, "reinforced_coil", 2, List.of(), Map.of('C', "COPPER_INGOT", 'I', "IRON_INGOT"), "fn_amplification_basics"));
        registerRecipe(api, new RecipeDefinition("amplification_module_recipe", RecipeDefinition.Type.SHAPED, "amplification_module", 1, List.of("CRC", "RAR", "CRC"), Map.of('C', "mechanium:reinforced_coil", 'R', "REDSTONE", 'A', "mechanium:amplifier_core"), "fn_amplification_tuning"));
        registerRecipe(api, new RecipeDefinition("amplification_station_recipe", RecipeDefinition.Type.SHAPED, "amplification_station", 1, List.of("ICI", "RDR", "III"), Map.of('I', "IRON_INGOT", 'C', "mechanium:amplifier_core", 'R', "REDSTONE", 'D', "DISPENSER"), "fn_amplification_tuning"));
        registerMachine(api, new MachineDefinition("amplification_station", "Estação de amplificação", "amplification_station", "DISPENSER", "mechanium:amplifier_core", 1, "amplification_module", 1, 1000, "fn_amplification_tuning"));
    }
}
