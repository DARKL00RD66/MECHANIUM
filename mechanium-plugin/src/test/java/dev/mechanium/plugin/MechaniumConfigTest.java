package dev.mechanium.plugin;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class MechaniumConfigTest {
    @Test
    void bundledYamlKeepsModulesFluidsAndMachineSettingsAtTheirOwnPaths() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(stream, "The plugin config.yml must be packaged in the JAR.");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            assertTrue(config.getBoolean("modules.fast-machines.enabled"));
            assertTrue(config.getBoolean("modules.water-treatment.enabled"));
            assertTrue(config.getBoolean("modules.greenhouse.enabled"));

            assertEquals(16_000, config.getInt("fluids.tank-capacity-mb"));
            assertEquals(250, config.getInt("fluids.transfer-mb-per-cycle"));

            assertEquals(100, config.getInt("machines.wear.max-cycles"));
            assertEquals(1, config.getInt("machines.wear.cycles-per-operation"));
            assertFalse(config.getBoolean("machines.hardcore.enabled"));
        }
    }
}
