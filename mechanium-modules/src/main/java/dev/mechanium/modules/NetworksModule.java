package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Owner-protected item logistics using connected MECHANIUM conduits and crates. */
final class NetworksModule extends AbstractBuiltinModule {
    @Override public String id() { return "industrial-networks"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("industrial-logistics", "simple-storage"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("industrial_networks", "Redes logísticas industriais",
                        "electrification", 42, List.of("industrial_logistics", "modular_warehousing")),
                new TechnologyDefinition("automated_distribution", "Distribuição automatizada",
                        "modern", 54, List.of("industrial_networks"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("industrial_networks", "Redes industriais",
                "Conduítes e depósitos conectados para transporte de itens MECHANIUM"));
        registerItem(api, new ItemDefinition("industrial_conduit", "&bConduíte industrial", "DISPENSER",
                List.of("&8Conecta depósitos industriais", "&7Transfere itens MECHANIUM entre caixas da mesma rede"),
                0, 64, "industrial_networks", "industrial_networks"));
        registerItem(api, new ItemDefinition("network_crate", "&6Caixa de rede", "BARREL",
                List.of("&8Depósito protegido de uma rede industrial", "&7Conecte por conduítes para movimentar itens"),
                0, 16, "industrial_networks", "industrial_networks"));
        registerItem(api, new ItemDefinition("network_controller", "&eControlador de rede", "COMPARATOR",
                List.of("&8Componente de controle e expansão de redes"),
                0, 16, "industrial_networks", "automated_distribution"));

        registerRecipe(api, new RecipeDefinition("industrial_conduit_recipe", RecipeDefinition.Type.SHAPED,
                "industrial_conduit", 8, List.of(" I ", "HCH", " I "),
                Map.of('I', "IRON_INGOT", 'H', "HOPPER", 'C', "REDSTONE"), "industrial_networks"));
        registerRecipe(api, new RecipeDefinition("network_crate_recipe", RecipeDefinition.Type.SHAPED,
                "network_crate", 2, List.of("III", "IBI", "III"),
                Map.of('I', "IRON_INGOT", 'B', "BARREL"), "industrial_networks"));
        registerRecipe(api, new RecipeDefinition("network_controller_recipe", RecipeDefinition.Type.SHAPELESS,
                "network_controller", 1, List.of(),
                Map.of('C', "COMPARATOR", 'R', "REDSTONE", 'I', "IRON_INGOT"), "automated_distribution"));
    }
}
