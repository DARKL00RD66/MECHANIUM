package dev.mechanium.api;

/**
 * Atomic registration scope for definitions owned by one built-in module or external addon.
 * Call commit only after the owner's entire onEnable callback succeeds.
 */
public interface MechaniumContentRegistrationScope extends AutoCloseable {
    void commit();
    @Override void close();

    static MechaniumContentRegistrationScope noop() {
        return new MechaniumContentRegistrationScope() {
            @Override public void commit() { }
            @Override public void close() { }
        };
    }
}
