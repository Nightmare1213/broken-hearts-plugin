package com.example.brokenhearts;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class BrokenHeartsPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        HeartItemService itemService = new HeartItemService(this);

        HeartEquipListener listener = new HeartEquipListener(this, itemService);
        listener.startTask();
        getServer().getPluginManager().registerEvents(listener, this);

        HeartCommand command = new HeartCommand(itemService);
        PluginCommand heartCommand = getCommand("heart");
        if (heartCommand != null) {
            heartCommand.setExecutor(command);
            heartCommand.setTabCompleter(command);
        } else {
            getLogger().warning("Command 'heart' is missing from plugin.yml");
        }

        registerRecipes(itemService);
    }

    private void registerRecipes(HeartItemService itemService) {
        for (HeartType type : HeartType.values()) {
            NamespacedKey key = new NamespacedKey(this, "heart_recipe_" + type.id());
            ShapedRecipe recipe = new ShapedRecipe(key, itemService.createHeart(type));
            recipe.shape("GDG", "DCD", "GDG");
            recipe.setIngredient('G', Material.GOLD_BLOCK);
            recipe.setIngredient('D', Material.DIAMOND_BLOCK);
            recipe.setIngredient('C', type.catalyst());
            getServer().addRecipe(recipe);
        }
    }
}
