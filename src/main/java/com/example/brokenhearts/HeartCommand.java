package com.example.brokenhearts;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HeartCommand implements CommandExecutor, TabCompleter {
    private final HeartItemService itemService;

    public HeartCommand(HeartItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("give")) {
            player.sendMessage(Component.text("Usage: /heart give <" + HeartType.commandList() + ">"));
            return true;
        }

        HeartType.fromString(args[1]).ifPresentOrElse(type -> {
            player.getInventory().addItem(itemService.createHeart(type));
            player.sendMessage(Component.text("You received a " + type.id() + " heart."));
        }, () -> player.sendMessage(Component.text("Unknown heart type.")));

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("give");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Arrays.stream(HeartType.values())
                    .map(HeartType::id)
                    .filter(id -> id.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
