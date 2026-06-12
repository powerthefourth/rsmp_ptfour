package me.rankedsmp.managers;

import me.rankedsmp.RankedSMP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class RankManager {

    private final RankedSMP plugin;
    private final ConfigManager cfg;

    // UUID → rank (1–20). Not present = UNRANKED
    private final Map<UUID, Integer> rankMap = new LinkedHashMap<>();
    // rank → UUID for O(1) reverse lookup
    private final Map<Integer, UUID> rankToPlayer = new HashMap<>();

    private boolean systemActive = false;
    private Scoreboard scoreboard;

    public RankManager(RankedSMP plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        setupTeams();
    }

    // ─────────────────────────────────────────────────
    // System Start / Stop
    // ─────────────────────────────────────────────────

    /**
     * Start the ranked system. If online players exist, distribute ranks
     * among up to maxRanked players randomly.
     */
    public void startRankedSMP(Player initiator) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) {
            initiator.sendMessage(cfg.getMessage("no-permission")
                    .replace("permission", "players online to rank"));
            return;
        }

        clearAllRanks();
        Collections.shuffle(online);
        int max = Math.min(cfg.getMaxRankedPlayers(), online.size());
        for (int i = 0; i < max; i++) {
            setPlayerRank(online.get(i).getUniqueId(), i + 1);
        }

        systemActive = true;

        // Update displays for all online
        for (Player p : Bukkit.getOnlinePlayers()) {
            updatePlayerDisplay(p);
            updatePlayerHealth(p);
        }

        Bukkit.broadcastMessage(cfg.getMessage("system-started"));
        plugin.saveConfig();
    }

    public void stopRankedSMP() {
        clearAllRanks();
        systemActive = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            updatePlayerDisplay(p);
            resetPlayerHealth(p);
        }
        Bukkit.broadcastMessage(cfg.getMessage("system-stopped"));
    }

    // ─────────────────────────────────────────────────
    // Rank Assignment
    // ─────────────────────────────────────────────────

    /**
     * Assign a rank to a player. Clears any previous occupant of that rank slot.
     */
    public void setPlayerRank(UUID uuid, int rank) {
        // Remove from old rank
        int old = rankMap.getOrDefault(uuid, -1);
        if (old != -1) {
            rankToPlayer.remove(old);
        }

        // Displace whoever already holds this rank
        UUID existing = rankToPlayer.get(rank);
        if (existing != null && !existing.equals(uuid)) {
            rankMap.remove(existing);
        }

        rankMap.put(uuid, rank);
        rankToPlayer.put(rank, uuid);
        cfg.saveRankToConfig(uuid, rank);

        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            updatePlayerDisplay(p);
            updatePlayerHealth(p);
            updateExtraInventoryAccess(p, old, rank);
        }
    }

    public void removePlayerRank(UUID uuid) {
        int rank = rankMap.getOrDefault(uuid, -1);
        if (rank == -1) return;
        rankMap.remove(uuid);
        rankToPlayer.remove(rank);
        cfg.removeRankFromConfig(uuid);

        Player p = Bukkit.getPlayer(uuid);
        if (p != null && p.isOnline()) {
            updatePlayerDisplay(p);
            resetPlayerHealth(p);
        }
    }

    /**
     * Core mechanic: killer steals victim's rank and victim gets killer's old rank.
     * Only fires if killer's rank > victim's rank (killer is lower-ranked).
     */
    public void handleRankSteal(Player killer, Player victim) {
        if (cfg.isKeepRanks()) return;

        int killerRank = getPlayerRank(killer);
        int victimRank = getPlayerRank(victim);

        if (killerRank == -1 || victimRank == -1) {
            // If killer is unranked but victim is ranked, killer takes victim's rank
            if (killerRank == -1 && victimRank != -1) {
                setPlayerRank(killer.getUniqueId(), victimRank);
                removePlayerRank(victim.getUniqueId());
                announceRankSteal(killer, victim, victimRank);
                return;
            }
            return; // Both unranked or only killer is ranked - no steal
        }

        // Only steal if killer has a LOWER rank (higher number)
        if (killerRank <= victimRank) {
            // Killer already outranks victim - no swap, but still award notification
            return;
        }

        // Swap ranks
        swapRanks(killer.getUniqueId(), victim.getUniqueId(), killerRank, victimRank);
        announceRankSteal(killer, victim, victimRank);
    }

    private void swapRanks(UUID killerUUID, UUID victimUUID, int killerRank, int victimRank) {
        // Direct manipulation to avoid double-displacement
        rankMap.put(killerUUID, victimRank);
        rankMap.put(victimUUID, killerRank);
        rankToPlayer.put(victimRank, killerUUID);
        rankToPlayer.put(killerRank, victimUUID);

        cfg.saveRankToConfig(killerUUID, victimRank);
        cfg.saveRankToConfig(victimUUID, killerRank);

        Player killer = Bukkit.getPlayer(killerUUID);
        Player victim = Bukkit.getPlayer(victimUUID);

        if (killer != null) {
            updatePlayerDisplay(killer);
            updatePlayerHealth(killer);
            updateExtraInventoryAccess(killer, killerRank, victimRank);
            killer.sendMessage(cfg.getRawMessage("rank-swap").replace("{rank}", String.valueOf(victimRank)));
        }
        if (victim != null) {
            updatePlayerDisplay(victim);
            updatePlayerHealth(victim);
            updateExtraInventoryAccess(victim, victimRank, killerRank);
            victim.sendMessage(cfg.colorize("&cYou lost rank &6#" + victimRank + " &cto " + (killer != null ? killer.getName() : "unknown") + ". You are now &6#" + killerRank));
        }
    }

    // ─────────────────────────────────────────────────
    // Queries
    // ─────────────────────────────────────────────────

    /** Returns 1–20 or -1 if unranked */
    public int getPlayerRank(Player player) {
        return rankMap.getOrDefault(player.getUniqueId(), -1);
    }

    public int getPlayerRank(UUID uuid) {
        return rankMap.getOrDefault(uuid, -1);
    }

    public boolean isRanked(Player player) {
        return rankMap.containsKey(player.getUniqueId());
    }

    public boolean isSystemActive() {
        return systemActive;
    }

    /** Returns the UUID of the player currently at a given rank, or null */
    public UUID getPlayerAtRank(int rank) {
        return rankToPlayer.get(rank);
    }

    /** Returns true if rank slot is occupied */
    public boolean isRankTaken(int rank) {
        return rankToPlayer.containsKey(rank);
    }

    /** Sorted list of ranked players (rank 1 first) */
    public List<Map.Entry<UUID, Integer>> getRankedPlayersSorted() {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(rankMap.entrySet());
        list.sort(Comparator.comparingInt(Map.Entry::getValue));
        return list;
    }

    public Map<UUID, Integer> getRankMap() {
        return Collections.unmodifiableMap(rankMap);
    }

    // ─────────────────────────────────────────────────
    // Display & Health
    // ─────────────────────────────────────────────────

    public void updatePlayerDisplay(Player player) {
        int rank = getPlayerRank(player);
        String prefix;
        String teamName;

        if (rank == -1) {
            prefix = ChatColor.GRAY + "[UNRANKED] ";
            teamName = "z_unranked";
        } else if (rank == 1) {
            prefix = ChatColor.GOLD + "" + ChatColor.BOLD + "[#1] " + ChatColor.RESET;
            teamName = "a_rank01";
        } else if (rank <= 5) {
            prefix = ChatColor.YELLOW + "[#" + rank + "] ";
            teamName = String.format("b_rank%02d", rank);
        } else if (rank <= 10) {
            prefix = ChatColor.GREEN + "[#" + rank + "] ";
            teamName = String.format("c_rank%02d", rank);
        } else {
            prefix = ChatColor.AQUA + "[#" + rank + "] ";
            teamName = String.format("d_rank%02d", rank);
        }

        // Remove from all rank teams first
        for (Team team : scoreboard.getTeams()) {
            team.removeEntry(player.getName());
        }

        // Assign to correct team
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        team.setPrefix(prefix);
        team.addEntry(player.getName());

        // Apply to all players so they see the tab/nametag update
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.setScoreboard(scoreboard);
        }
    }

    public void updatePlayerHealth(Player player) {
        int rank = getPlayerRank(player);
        double hp;
        if (rank == -1) {
            hp = 20.0; // Default 10 hearts for unranked
        } else {
            hp = cfg.getHealthForRank(rank);
        }

        try {
            var attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) {
                attr.setBaseValue(hp);
                // Clamp current health to new max
                if (player.getHealth() > hp) {
                    player.setHealth(hp);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not set health for " + player.getName() + ": " + e.getMessage());
        }
    }

    public void resetPlayerHealth(Player player) {
        try {
            var attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) {
                attr.setBaseValue(20.0);
            }
        } catch (Exception ignored) {}
    }

    public void initializePlayer(Player player) {
        updatePlayerDisplay(player);
        if (isRanked(player)) {
            updatePlayerHealth(player);
        }
    }

    // ─────────────────────────────────────────────────
    // Extra Inventory helper
    // ─────────────────────────────────────────────────

    private void updateExtraInventoryAccess(Player player, int oldRank, int newRank) {
        plugin.getExtraInventoryManager().onRankChange(player, oldRank, newRank);
    }

    // ─────────────────────────────────────────────────
    // Persistence
    // ─────────────────────────────────────────────────

    public void loadRanks() {
        Map<UUID, Integer> saved = cfg.loadAllRanksFromConfig();
        if (!saved.isEmpty()) {
            rankMap.putAll(saved);
            for (Map.Entry<UUID, Integer> e : saved.entrySet()) {
                rankToPlayer.put(e.getValue(), e.getKey());
            }
            systemActive = true;
            plugin.getLogger().info("Loaded " + saved.size() + " ranked players from config.");
        }
    }

    public void saveRanks() {
        cfg.clearAllRanksFromConfig();
        for (Map.Entry<UUID, Integer> e : rankMap.entrySet()) {
            cfg.saveRankToConfig(e.getKey(), e.getValue());
        }
    }

    // ─────────────────────────────────────────────────
    // Internal utilities
    // ─────────────────────────────────────────────────

    private void clearAllRanks() {
        rankMap.clear();
        rankToPlayer.clear();
        cfg.clearAllRanksFromConfig();
    }

    private void setupTeams() {
        // Pre-create teams with ordering prefix so tab list sorts correctly
        // Teams are created on-demand in updatePlayerDisplay but we set up base here
    }

    private void announceRankSteal(Player killer, Player victim, int stolenRank) {
        if (!cfg.isAnnounceRankSwaps()) return;
        String msg = cfg.getMessage("rank-stolen")
                .replace("{killer}", killer.getName())
                .replace("{victim}", victim.getName())
                .replace("{rank}", String.valueOf(stolenRank));
        Bukkit.broadcastMessage(msg);
    }

    // ─────────────────────────────────────────────────
    // Scoreboard rank display string helper
    // ─────────────────────────────────────────────────

    public String getRankDisplay(Player player) {
        int rank = getPlayerRank(player);
        if (rank == -1) return ChatColor.GRAY + "[UNRANKED]";
        if (rank == 1) return ChatColor.GOLD + "" + ChatColor.BOLD + "[#1]";
        if (rank <= 5) return ChatColor.YELLOW + "[#" + rank + "]";
        if (rank <= 10) return ChatColor.GREEN + "[#" + rank + "]";
        return ChatColor.AQUA + "[#" + rank + "]";
    }

    public String getRankDisplayPlain(int rank) {
        if (rank == -1) return "[UNRANKED]";
        return "[#" + rank + "]";
    }
}
