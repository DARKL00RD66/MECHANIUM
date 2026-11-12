package dev.mechanium.modules;

import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;

final class EnvironmentModule extends AbstractBuiltinModule {
    @Override public String id() { return "environment"; }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("waste_management", "Gestão de resíduos", "industrial_revolution", 20, List.of()),
            new TechnologyDefinition("emissions_control", "Controle de emissões", "modern", 45, List.of("waste_management"))
        );
    }
}