package dev.mechanium.modules;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinModulesTest {
    @Test
    void includesAllRegisteredAddonDomainModules() {
        Set<String> ids = BuiltinModules.create().stream()
                .map(module -> module.id())
                .collect(Collectors.toSet());

        assertTrue(ids.containsAll(Set.of(
                "extra-gear",
                "eco-power",
                "sfcalc",
                "liquid",
                "advanced-tech",
                "simple-storage",
                "sensible-toolbox",
                "agriculture",
                "exotic-garden",
                "dank-tech2",
                "culinary",
                "environment",
                "electric-spawners",
                "fast-machines",
                "hardcore-slimefun",
                "air-filtration",
                "advanced-metallurgy",
                "pressure-engineering",
                "industrial-automation",
                "industrial-logistics",
                "forestry",
                "textile-industry",
                "biofuel",
                "construction-materials",
                "industrial-networks",
                "better-reactor",
                "material-generators",
                "water-treatment",
                "greenhouse",
                "industrial-recycling",
                "ore-chunks"
        )));
    }

    @Test
    void allBuiltinModuleDependenciesResolveWithoutCycles() {
        assertDoesNotThrow(() -> ModuleManager.orderByDependencies(BuiltinModules.create()));
    }
}
