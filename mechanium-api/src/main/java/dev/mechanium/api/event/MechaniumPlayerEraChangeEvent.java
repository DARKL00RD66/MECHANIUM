package dev.mechanium.api.event;

import org.bukkit.event.HandlerList;

import java.util.Objects;
import java.util.UUID;

/** Fired after a player's era is persisted. The previous era is null for a new player. */
public final class MechaniumPlayerEraChangeEvent extends MechaniumProgressionEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final String previousEraId;
    private final String newEraId;

    public MechaniumPlayerEraChangeEvent(UUID playerId, String previousEraId, String newEraId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.previousEraId = previousEraId;
        this.newEraId = Objects.requireNonNull(newEraId, "newEraId");
    }

    public UUID playerId() { return playerId; }
    public String previousEraId() { return previousEraId; }
    public String newEraId() { return newEraId; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
