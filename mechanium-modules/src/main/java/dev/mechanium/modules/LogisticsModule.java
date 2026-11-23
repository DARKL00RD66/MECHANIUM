package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;

final class LogisticsModule extends AbstractBuiltinModule {
    @Override public String id() { return "industrial-logistics"; }
    @Override public java.util.Set<String> dependencies() { return java.util.Set.of("industry"); }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(new TechnologyDefinition("industrial_logistics", "Logística industrial",
                "modern", 38, List.of("assembly_line")));
    }
    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("industrial_logistics", "Logística industrial",
                "Transporte, armazenamento e distribuição de materiais"));
        registerItem(api, new ItemDefinition("transport_crate", "&6Caixa de transporte", "BARREL",
                List.of("&8Recipiente industrial de carga"), 0, 16,
                "industrial_logistics", "industrial_logistics"));
        registerItem(api, new ItemDefinition("cargo_router", "&eRoteador de carga", "DISPENSER",
                List.of("&8Equipamento de distribuição de materiais"), 0, 1,
                "industrial_logistics", "industrial_logistics"));
        registerRecipe(api, new RecipeDefinition("transport_crate_recipe", RecipeDefinition.Type.SHAPELESS,
                "transport_crate", 2, List.of(),
                Map.of('W', "OAK_PLANKS", 'I', "IRON_INGOT", 'C', "CHEST"),
                "industrial_logistics"));
        registerRecipe(api, new RecipeDefinition("cargo_router_recipe", RecipeDefinition.Type.SHAPELESS,
                "cargo_router", 1, List.of(),
                Map.of('H', "HOPPER", 'C', "COMPARATOR", 'I', "IRON_INGOT"),
                "industrial_logistics"));
        registerMachine(api, new MachineDefinition("cargo_router", "Roteador de carga", "cargo_router",
                "DISPENSER", "IRON_INGOT", 2, "transport_crate", 1, 1200, "industrial_logistics"));
    }
}