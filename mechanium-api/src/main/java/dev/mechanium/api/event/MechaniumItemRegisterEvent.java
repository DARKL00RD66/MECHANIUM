package dev.mechanium.api.event;
import dev.mechanium.api.model.ItemDefinition;
import org.bukkit.event.HandlerList;
import java.util.Objects;
public final class MechaniumItemRegisterEvent extends MechaniumContentEvent {
 private static final HandlerList HANDLERS=new HandlerList();private final ItemDefinition item;
 public MechaniumItemRegisterEvent(ItemDefinition value){item=Objects.requireNonNull(value);}public ItemDefinition item(){return item;}
 @Override public HandlerList getHandlers(){return HANDLERS;}public static HandlerList getHandlerList(){return HANDLERS;}
}