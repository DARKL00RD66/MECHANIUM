package dev.mechanium.modules;

import dev.mechanium.api.model.TechnologyDefinition;
import java.util.List;

final class EnergyModule extends AbstractBuiltinModule {
    @Override public String id() { return "energy"; }
    @Override protected List<TechnologyDefinition> technologies() {
        return List.of(
            new TechnologyDefinition("water_power", "Energia hidráulica", "medieval", 10, List.of()),
            new TechnologyDefinition("steam_turbine", "Turbina a vapor", "industrial_revolution", 40, List.of("water_power", "steam_power")),
            new TechnologyDefinition("electrical_grid", "Rede elétrica", "electrification", 65, List.of("steam_turbine"))
        );
    }
}