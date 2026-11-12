package dev.mechanium.api.event;

import dev.mechanium.api.model.TechnologyDefinition;
import org.bukkit.event.HandlerList;

import java.util.Objects;
import java.util.UUID;

/** Fired after a technology has been atomically unlocked and its cost persisted. */
public final class MechaniumTechnologyUnlockedEvent extends MechaniumProgressionEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerId;
    private final TechnologyDefinition technology;
    private final long remainingResearchPoints;

    public MechaniumTechnologyUnlockedEvent(UUID playerId, TechnologyDefinition technology, long remainingResearchPoints) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.technology = Objects.requireNonNull(technology, "technology");
        if (remainingResearchPoints < 0) throw new IllegalArgumentException("remainingResearchPoints cannot be negative");
        this.remainingResearchPoints = remainingResearchPoints;
    }

    public UUID playerId() { return playerId; }
    public TechnologyDefinition technology() { return technology; }
    public long remainingResearchPoints() { return remainingResearchPoints; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
