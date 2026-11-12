package dev.mechanium.api.event;

import org.bukkit.event.Event;

/** Base class for public events emitted by the MECHANIUM progression engine. */
public abstract class MechaniumProgressionEvent extends Event {
    protected MechaniumProgressionEvent() {
        super(false);
    }
}
