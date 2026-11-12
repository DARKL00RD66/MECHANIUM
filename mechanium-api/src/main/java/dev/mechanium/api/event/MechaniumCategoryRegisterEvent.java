package dev.mechanium.api.event;
import dev.mechanium.api.model.ContentCategory;
import org.bukkit.event.HandlerList;
import java.util.Objects;
public final class MechaniumCategoryRegisterEvent extends MechaniumContentEvent {
 private static final HandlerList HANDLERS=new HandlerList();private final ContentCategory category;
 public MechaniumCategoryRegisterEvent(ContentCategory value){category=Objects.requireNonNull(value);}public ContentCategory category(){return category;}
 @Override public HandlerList getHandlers(){return HANDLERS;}public static HandlerList getHandlerList(){return HANDLERS;}
}