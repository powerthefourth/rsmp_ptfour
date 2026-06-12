package me.rankedsmp.listeners;

import me.rankedsmp.RankedSMP;
import me.rankedsmp.managers.RankManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final RankedSMP plugin;
    private final RankManager rankManager;

    public ChatListener(RankedSMP plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String rankDisplay = rankManager.getRankDisplay(player);
        // Format: [#1] PlayerName: message
        event.setFormat(rankDisplay + " §r§f" + player.getName() + "§7: §f%2$s");
    }
}
