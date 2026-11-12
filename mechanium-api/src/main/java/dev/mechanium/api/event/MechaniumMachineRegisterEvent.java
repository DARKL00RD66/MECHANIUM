package dev.mechanium.api.event;

import dev.mechanium.api.model.MachineDefinition;
import org.bukkit.event.HandlerList;

import java.util.Objects;

/** Fired after an industrial machine definition has been registered. */
public final class MechaniumMachineRegisterEvent extends MechaniumContentEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final MachineDefinition machine;

    public MechaniumMachineRegisterEvent(MachineDefinition machine) {
        this.machine = Objects.requireNonNull(machine, "machine");
    }

    public MachineDefinition machine() { return machine; }

    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
