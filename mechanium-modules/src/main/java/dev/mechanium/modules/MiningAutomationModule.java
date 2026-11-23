package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Native MECHANIUM module: mining-automation. */
final class MiningAutomationModule extends AbstractBuiltinModule {
    @Override public String id() { return "mining-automation"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industry", "geology", "energy"); }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("automated_surveying", "Prospecção automatizada", "industrial_revolution", 38, List.of()),
            new TechnologyDefinition("automated_extraction", "Extração automatizada", "modern", 54, List.of())
        );
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("mining_automation_goods", "Mineração automatizada", "Componentes fictícios de prospecção e processamento mineral automatizado"));
        registerItem(api, new ItemDefinition("survey_chip", "&eChip de prospecção", "COMPASS", List.of("&8Conteúdo do módulo mining-automation"), 0, 64, "mining_automation_goods", "automated_surveying"));
        registerItem(api, new ItemDefinition("drill_casing", "&eCarcaça de perfuração", "IRON_BLOCK", List.of("&8Conteúdo do módulo mining-automation"), 0, 64, "mining_automation_goods", "automated_surveying"));
        registerItem(api, new ItemDefinition("mining_module", "&eMódulo minerador", "PISTON", List.of("&8Conteúdo do módulo mining-automation"), 0, 64, "mining_automation_goods", "automated_extraction"));
        registerItem(api, new ItemDefinition("mining_assembler", "&eMontadora de mineração", "DISPENSER", List.of("&8Conteúdo do módulo mining-automation"), 0, 64, "mining_automation_goods", "automated_extraction"));
        registerRecipe(api, new RecipeDefinition("survey_chip_recipe", RecipeDefinition.Type.SHAPELESS, "survey_chip", 1, List.of(), Map.of('C', "COMPASS", 'R', "REDSTONE"), "automated_surveying"));
        registerRecipe(api, new RecipeDefinition("drill_casing_recipe", RecipeDefinition.Type.SHAPELESS, "drill_casing", 1, List.of(), Map.of('I', "IRON_INGOT", 'C', "COPPER_INGOT"), "automated_surveying"));
        registerRecipe(api, new RecipeDefinition("mining_module_recipe", RecipeDefinition.Type.SHAPED, "mining_module", 1, List.of(" I ", "RPI", " I "), Map.of('I', "IRON_INGOT", 'R', "REDSTONE", 'P', "PISTON"), "automated_extraction"));
        registerRecipe(api, new RecipeDefinition("mining_assembler_recipe", RecipeDefinition.Type.SHAPED, "mining_assembler", 1, List.of("ICI", "RDR", "III"), Map.of('I', "IRON_INGOT", 'C', "mechanium:drill_casing", 'R', "REDSTONE", 'D', "DISPENSER"), "automated_extraction"));
        registerMachine(api, new MachineDefinition("mining_assembler", "Montadora de mineração", "mining_assembler", "DISPENSER", "mechanium:drill_casing", 1, "mining_module", 1, 1800, "automated_extraction"));
    }
}
