package dev.mechanium.api.event;

import org.bukkit.event.HandlerList;

import java.util.Objects;
import java.util.UUID;

/** Fired after a player's research-point balance changes. */
public final class MechaniumResearchPointsChangeEvent extends MechaniumProgressionEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final long previousPoints;
    private final long newPoints;

    public MechaniumResearchPointsChangeEvent(UUID playerId, long previousPoints, long newPoints) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        if (previousPoints < 0 || newPoints < 0) throw new IllegalArgumentException("Research points cannot be negative");
        this.previousPoints = previousPoints;
        this.newPoints = newPoints;
    }

    public UUID playerId() { return playerId; }
    public long previousPoints() { return previousPoints; }
    public long newPoints() { return newPoints; }
    public long difference() { return newPoints - previousPoints; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
