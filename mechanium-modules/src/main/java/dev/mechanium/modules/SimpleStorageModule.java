package dev.mechanium.modules;

import dev.mechanium.api.model.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Additional protected storage node using the existing persistent owner/trust system. */
final class SimpleStorageModule extends AbstractBuiltinModule {
    @Override public String id() { return "simple-storage"; }
    @Override public boolean optional() { return true; }
    @Override public Set<String> dependencies() { return Set.of("storage"); }

    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
                new TechnologyDefinition("modular_warehousing", "Armazenamento modular", "industrial_revolution", 34,
                        List.of("standardized_logistics"))
        );
    }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new ContentCategory("modular_storage", "Armazenamento modular", "Nós adicionais de armazenamento pessoal"));
        registerItem(api, new ItemDefinition("simple_storage_node", "&bNó de armazenamento", "BARREL",
                List.of("&8Armazém protegido adicional", "&7Usa proprietário e permissões por UUID"),
                0, 1, "modular_storage", "modular_warehousing"));
        registerRecipe(api, new RecipeDefinition("simple_storage_node_recipe", RecipeDefinition.Type.SHAPED,
                "simple_storage_node", 1, List.of("III", "IBI", "III"),
                Map.of('I', "IRON_INGOT", 'B', "BARREL"), "modular_warehousing"));
    }
}
