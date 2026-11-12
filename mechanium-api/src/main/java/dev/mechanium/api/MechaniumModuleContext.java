package dev.mechanium.api;

import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.Path;
import java.util.logging.Logger;

/** Runtime services exposed to internal modules. */
public record MechaniumModuleContext(MechaniumApi api, JavaPlugin hostPlugin, Path dataDirectory, Logger logger) {
    public MechaniumModuleContext {
        if (api == null || hostPlugin == null || dataDirectory == null || logger == null)
            throw new IllegalArgumentException("Module context fields cannot be null");
    }
}