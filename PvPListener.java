package me.rankedsmp.listeners;

import me.rankedsmp.RankedSMP;
import me.rankedsmp.managers.RankManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PvPListener implements Listener {

    private final RankedSMP plugin;
    private final RankManager rankManager;

    public PvPListener(RankedSMP plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Only process PvP kills
        if (killer == null || killer.equals(victim)) return;
        if (!rankManager.isSystemActive()) return;

        // Trigger rank steal logic
        rankManager.handleRankSteal(killer, victim);
    }
}
