package dev.mechanium.api.model;
import java.util.Locale;
import java.util.Objects;
public record ContentCategory(String id,String displayName,String description) {
 public ContentCategory { id=norm(id); displayName=Objects.requireNonNull(displayName).trim(); description=Objects.requireNonNullElse(description,"").trim(); if(displayName.isEmpty())throw new IllegalArgumentException("displayName cannot be empty"); }
 private static String norm(String v){Objects.requireNonNull(v);String n=v.trim().toLowerCase(Locale.ROOT);if(!n.matches("[a-z0-9][a-z0-9_.-]{1,63}"))throw new IllegalArgumentException("Invalid category id: "+v);return n;}
}