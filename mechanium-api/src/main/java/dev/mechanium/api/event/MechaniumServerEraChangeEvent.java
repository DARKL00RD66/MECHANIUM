package dev.mechanium.api.event;

import org.bukkit.event.HandlerList;

/** Fired after the server-wide historical era has been persisted. */
public final class MechaniumServerEraChangeEvent extends MechaniumProgressionEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String previousEraId;
    private final String newEraId;

    public MechaniumServerEraChangeEvent(String previousEraId, String newEraId) {
        this.previousEraId = previousEraId;
        this.newEraId = java.util.Objects.requireNonNull(newEraId, "newEraId");
    }

    public String previousEraId() { return previousEraId; }
    public String newEraId() { return newEraId; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
