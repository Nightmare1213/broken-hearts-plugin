package com.example.brokenhearts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HeartItemService {
    private final NamespacedKey heartKey;

    public HeartItemService(JavaPlugin plugin) {
        this.heartKey = new NamespacedKey(plugin, "heart_type");
    }

    public ItemStack createHeart(HeartType type) {
        ItemStack item = new ItemStack(type.itemMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(type.displayName());

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(type.abilityName(), NamedTextColor.GOLD));
        lore.add(Component.text(type.abilityDescription(), NamedTextColor.GRAY));
        if (type.isBossHeart()) {
            lore.add(Component.text("Boss scaling: +1 tier per 5 kills (max tier 5).", NamedTextColor.LIGHT_PURPLE));
        }
        lore.add(Component.text("Equip this in your off-hand.", NamedTextColor.DARK_GRAY));
        meta.lore(lore);

        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(heartKey, PersistentDataType.STRING, type.id());
        item.setItemMeta(meta);
        return item;
    }

    public Optional<HeartType> detectHeart(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return Optional.empty();
        }

        ItemMeta meta = item.getItemMeta();
        String id = meta.getPersistentDataContainer().get(heartKey, PersistentDataType.STRING);
        if (id == null) {
            return Optional.empty();
        }

        return HeartType.fromString(id);
    }
}
