package dev.mechanium.api;

import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.Path;
import java.util.logging.Logger;

/** Services exposed to a third-party addon. */
public record MechaniumAddonContext(MechaniumApi api, JavaPlugin hostPlugin, Path dataDirectory, Logger logger) {
    public MechaniumAddonContext {
        if (api == null || hostPlugin == null || dataDirectory == null || logger == null)
            throw new IllegalArgumentException("Addon context fields cannot be null");
    }
}