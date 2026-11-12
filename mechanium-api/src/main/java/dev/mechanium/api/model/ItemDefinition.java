package dev.mechanium.api.model;
import org.bukkit.Material;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
public record ItemDefinition(String id,String displayName,String material,List<String> lore,int customModelData,int maxStackSize,String categoryId,String requiredTechnologyId) {
 public ItemDefinition { id=norm(id,"id");displayName=Objects.requireNonNull(displayName).trim();if(displayName.isEmpty())throw new IllegalArgumentException("displayName cannot be empty");material=Objects.requireNonNull(material).trim().toUpperCase(Locale.ROOT);Material m=Material.matchMaterial(material);if(m==null||material.equals("AIR")||material.equals("CAVE_AIR")||material.equals("VOID_AIR"))throw new IllegalArgumentException("Unknown or non-item material: "+material);lore=lore==null?List.of():lore.stream().map(v->Objects.requireNonNull(v).trim()).toList();if(customModelData<0)throw new IllegalArgumentException("customModelData cannot be negative");if(maxStackSize<1||maxStackSize>99)throw new IllegalArgumentException("maxStackSize must be 1..99");categoryId=norm(categoryId,"categoryId");requiredTechnologyId=requiredTechnologyId==null||requiredTechnologyId.isBlank()?null:norm(requiredTechnologyId,"requiredTechnologyId");}
 private static String norm(String v,String f){Objects.requireNonNull(v,f);String n=v.trim().toLowerCase(Locale.ROOT);if(!n.matches("[a-z0-9][a-z0-9_.-]{1,95}"))throw new IllegalArgumentException("Invalid "+f+": "+v);return n;}
}