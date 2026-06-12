package me.rankedsmp.listeners;

import me.rankedsmp.RankedSMP;
import me.rankedsmp.managers.RankManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class XPListener implements Listener {

    private final RankedSMP plugin;
    private final RankManager rankManager;

    public XPListener(RankedSMP plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onXPGain(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        int rank = rankManager.getPlayerRank(player);
        if (rank == -1) return; // Unranked: no multiplier

        double multiplier = plugin.getConfigManager().getXPMultiplierForRank(rank);
        int original = event.getAmount();
        if (original <= 0) return;

        event.setAmount((int) Math.round(original * multiplier));
    }
}
