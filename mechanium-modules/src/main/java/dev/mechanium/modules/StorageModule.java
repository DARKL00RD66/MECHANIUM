package dev.mechanium.modules;

import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;

final class StorageModule extends AbstractBuiltinModule {
    @Override public String id() { return "storage"; }

    @Override protected void registerExtra(dev.mechanium.api.MechaniumModuleContext context) {
        var api = context.api();
        registerCategory(api, new dev.mechanium.api.model.ContentCategory(
                "private_storage", "Armazenamento privado", "Armazéns protegidos por proprietário"));
        registerItem(api, new dev.mechanium.api.model.ItemDefinition(
                "private_storage_crate", "&6Caixa de armazenamento privada", "BARREL",
                java.util.List.of("&8Armazém pessoal protegido", "&7Apenas o proprietário e administradores podem acessar."),
                0, 1, "private_storage", "warehouse_accounting"));
        registerRecipe(api, new dev.mechanium.api.model.RecipeDefinition(
                "private_storage_crate_recipe", dev.mechanium.api.model.RecipeDefinition.Type.SHAPED,
                "private_storage_crate", 1,
                java.util.List.of("III", "IBI", "III"),
                java.util.Map.of('I', "IRON_INGOT", 'B', "BARREL"),
                "warehouse_accounting"));
    }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("warehouse_accounting", "Contabilidade de armazéns", "renaissance", 12, List.of()),
            new TechnologyDefinition("standardized_logistics", "Logística padronizada", "industrial_revolution", 32, List.of("warehouse_accounting"))
        );
    }
}