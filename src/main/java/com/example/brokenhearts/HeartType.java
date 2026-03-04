package com.example.brokenhearts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum HeartType {
    FLAME("flame", NamedTextColor.RED, "Inferno Brand", "Melee hits burn enemies for 8s and deal +2 damage.", false, Material.BLAZE_ROD),
    SWIFT("swift", NamedTextColor.AQUA, "Tempest Dash", "Press swap-hands to dash hard and gain Speed II (4s cooldown).", false, Material.FEATHER),
    STONE("stone", NamedTextColor.GRAY, "Titan Guard", "Reduces incoming damage by 45%.", false, Material.IRON_BLOCK),
    NIGHT("night", NamedTextColor.DARK_PURPLE, "Abyss Pulse", "Right click to reveal + slow nearby monsters (10s cooldown).", false, Material.ENDER_EYE),
    VAMPIRIC("vampiric", NamedTextColor.DARK_RED, "Blood Feast", "Steal hearts from enemies on hit and heal yourself.", false, Material.GHAST_TEAR),
    THUNDER("thunder", NamedTextColor.YELLOW, "Stormcaller", "Melee hits apply a storm that repeatedly shocks targets.", false, Material.LIGHTNING_ROD),
    FROST("frost", NamedTextColor.BLUE, "Glacial Prison", "Right click to freeze monsters in place for a short time.", false, Material.BLUE_ICE),
    DRAGON("dragon", NamedTextColor.LIGHT_PURPLE, "Dragon Roar", "Boss Heart: gets stronger with kills while equipped.", true, Material.DRAGON_BREATH),
    WARDEN("warden", NamedTextColor.DARK_AQUA, "Deep Silence", "Boss Heart: gets stronger with kills while equipped.", true, Material.SCULK_CATALYST),
    WITHER("wither", NamedTextColor.DARK_GRAY, "Wither Reign", "Boss Heart: gets stronger with kills while equipped.", true, Material.WITHER_ROSE);

    private final String id;
    private final NamedTextColor color;
    private final String abilityName;
    private final String abilityDescription;
    private final boolean bossHeart;
    private final Material catalyst;

    HeartType(String id, NamedTextColor color, String abilityName, String abilityDescription, boolean bossHeart, Material catalyst) {
        this.id = id;
        this.color = color;
        this.abilityName = abilityName;
        this.abilityDescription = abilityDescription;
        this.bossHeart = bossHeart;
        this.catalyst = catalyst;
    }

    public String id() {
        return id;
    }

    public String abilityName() {
        return abilityName;
    }

    public String abilityDescription() {
        return abilityDescription;
    }

    public boolean isBossHeart() {
        return bossHeart;
    }

    public Material catalyst() {
        return catalyst;
    }

    public Component displayName() {
        String readable = id.substring(0, 1).toUpperCase() + id.substring(1);
        return Component.text(readable + " Heart", color);
    }

    public Material itemMaterial() {
        return Material.HEART_OF_THE_SEA;
    }

    public static String commandList() {
        return Arrays.stream(values()).map(HeartType::id).collect(Collectors.joining("|"));
    }

    public static Optional<HeartType> fromString(String input) {
        return Arrays.stream(values())
                .filter(type -> type.id.equalsIgnoreCase(input))
                .findFirst();
    }
}
