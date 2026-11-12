package dev.mechanium.api.event;

import dev.mechanium.api.model.MobDropDefinition;
import org.bukkit.event.HandlerList;

import java.util.Objects;

/** Fired when a mob drop rule is added to the public registry. */
public final class MechaniumMobDropRegisterEvent extends MechaniumContentEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final MobDropDefinition drop;

    public MechaniumMobDropRegisterEvent(MobDropDefinition drop) {
        this.drop = Objects.requireNonNull(drop, "drop");
    }

    public MobDropDefinition drop() { return drop; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
