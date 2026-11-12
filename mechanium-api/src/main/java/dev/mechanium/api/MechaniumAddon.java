package dev.mechanium.api;

/** Lifecycle contract for external addons loaded by the MECHANIUM addon manager. */
public interface MechaniumAddon {
    void onEnable(MechaniumAddonContext context) throws Exception;
    void onDisable();
}