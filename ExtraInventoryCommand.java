package me.rankedsmp.commands;

import me.rankedsmp.RankedSMP;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ExtraInventoryCommand implements CommandExecutor {

    private final RankedSMP plugin;

    public ExtraInventoryCommand(RankedSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by a player.");
            return true;
        }

        if (!plugin.getRankManager().isSystemActive()) {
            player.sendMessage("§cRankedSMP is not currently active.");
            return true;
        }

        plugin.getExtraInventoryManager().openExtraInventory(player);
        return true;
    }
}
