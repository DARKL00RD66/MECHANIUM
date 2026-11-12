package dev.mechanium.modules;

import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;

final class StorageModule extends AbstractBuiltinModule {
    @Override public String id() { return "storage"; }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("warehouse_accounting", "Contabilidade de armazéns", "renaissance", 12, List.of()),
            new TechnologyDefinition("standardized_logistics", "Logística padronizada", "industrial_revolution", 32, List.of("warehouse_accounting"))
        );
    }
}