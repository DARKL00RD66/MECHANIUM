package dev.mechanium.modules;

import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;

final class IndustryModule extends AbstractBuiltinModule {
    @Override public String id() { return "industry"; }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("basic_mechanics", "Mecânica básica", "renaissance", 10, List.of()),
            new TechnologyDefinition("steam_power", "Energia a vapor", "industrial_revolution", 35, List.of("basic_mechanics")),
            new TechnologyDefinition("mechanized_production", "Produção mecanizada", "industrial_revolution", 55, List.of("steam_power"))
        );
    }
}