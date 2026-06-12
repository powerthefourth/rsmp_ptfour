package me.rankedsmp.listeners;

import me.rankedsmp.RankedSMP;
import me.rankedsmp.managers.RankManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final RankedSMP plugin;
    private final RankManager rankManager;

    public PlayerListener(RankedSMP plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Slight delay to ensure display applies after full login
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            rankManager.initializePlayer(player);
        }, 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Save extra inventory on quit
        plugin.getExtraInventoryManager().savePlayerInventory(player.getUniqueId());
    }
}
