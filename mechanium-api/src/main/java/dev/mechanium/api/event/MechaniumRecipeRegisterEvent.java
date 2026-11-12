package dev.mechanium.api.event;
import dev.mechanium.api.model.RecipeDefinition;
import org.bukkit.event.HandlerList;
import java.util.Objects;
public final class MechaniumRecipeRegisterEvent extends MechaniumContentEvent {
 private static final HandlerList HANDLERS=new HandlerList();private final RecipeDefinition recipe;
 public MechaniumRecipeRegisterEvent(RecipeDefinition value){recipe=Objects.requireNonNull(value);}public RecipeDefinition recipe(){return recipe;}
 @Override public HandlerList getHandlers(){return HANDLERS;}public static HandlerList getHandlerList(){return HANDLERS;}
}