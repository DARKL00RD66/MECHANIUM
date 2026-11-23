package dev.mechanium.modules;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinModulesTest {
    @Test
    void includesAllSevenNewAddonDomainModules() {
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
                "environment"
        )));
    }

    @Test
    void allBuiltinModuleDependenciesResolveWithoutCycles() {
        assertDoesNotThrow(() -> ModuleManager.orderByDependencies(BuiltinModules.create()));
    }
}
