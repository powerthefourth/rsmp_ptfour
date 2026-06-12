package me.rankedsmp.managers;

import me.rankedsmp.RankedSMP;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfigManager {

    private final RankedSMP plugin;
    private FileConfiguration config;

    public ConfigManager(RankedSMP plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // ── Gameplay ─────────────────────────────────────

    public boolean isKeepRanks() {
        return config.getBoolean("gameplay.keep-ranks", false);
    }

    public int getMaxRankedPlayers() {
        return config.getInt("gameplay.max-ranked-players", 20);
    }

    public boolean isAnnounceRankSwaps() {
        return config.getBoolean("gameplay.announce-rank-swaps", true);
    }

    public boolean isShowNametags() {
        return config.getBoolean("gameplay.show-nametags", true);
    }

    // ── Health ────────────────────────────────────────

    public double getRank1MaxHealth() {
        return config.getInt("health.rank-1-hearts", 40);
    }

    public double getRank20MaxHealth() {
        return config.getInt("health.rank-20-hearts", 21);
    }

    /**
     * Returns max health (in half-hearts / HP) for a given rank 1–20.
     * Linearly interpolates between rank1 and rank20 values.
     */
    public double getHealthForRank(int rank) {
        if (rank < 1) rank = 1;
        if (rank > 20) rank = 20;
        double max = getRank1MaxHealth();
        double min = getRank20MaxHealth();
        // rank 1 → max, rank 20 → min
        return max - (max - min) * ((rank - 1.0) / 19.0);
    }

    // ── XP ────────────────────────────────────────────

    public double getRank1XPMultiplier() {
        return config.getDouble("xp.rank-1-multiplier", 3.0);
    }

    public double getRank20XPMultiplier() {
        return config.getDouble("xp.rank-20-multiplier", 1.1);
    }

    public double getXPMultiplierForRank(int rank) {
        if (rank < 1) rank = 1;
        if (rank > 20) rank = 20;
        double max = getRank1XPMultiplier();
        double min = getRank20XPMultiplier();
        return max - (max - min) * ((rank - 1.0) / 19.0);
    }

    // ── Potions ───────────────────────────────────────

    public double getRank1PotionMultiplier() {
        return config.getDouble("potions.rank-1-multiplier", 2.0);
    }

    public double getRank20PotionMultiplier() {
        return config.getDouble("potions.rank-20-multiplier", 1.05);
    }

    public boolean isScaleNegativeEffects() {
        return config.getBoolean("potions.scale-negative-effects", true);
    }

    public double getPotionMultiplierForRank(int rank) {
        if (rank < 1) rank = 1;
        if (rank > 20) rank = 20;
        double max = getRank1PotionMultiplier();
        double min = getRank20PotionMultiplier();
        return max - (max - min) * ((rank - 1.0) / 19.0);
    }

    // ── Extra Inventory ───────────────────────────────

    public int getExtraInventorySlotsForRank(int rank) {
        if (rank < 1 || rank > 10) return 0;
        int slots = config.getInt("extra-inventory.rank-" + rank + "-slots", 0);
        // Round to nearest valid inventory size (9, 18, 27, 36, 45, 54)
        if (slots <= 0) return 0;
        if (slots <= 9) return 9;
        if (slots <= 18) return 18;
        if (slots <= 27) return 27;
        if (slots <= 36) return 36;
        if (slots <= 45) return 45;
        return 54;
    }

    // ── Hammer ────────────────────────────────────────

    public int getHammerHitsToCharge() {
        return config.getInt("hammer.hits-to-charge", 4);
    }

    public double getHammerVerdictDamageMultiplier() {
        return config.getDouble("hammer.verdict-damage-multiplier", 2.0);
    }

    public double getHammerVerdictAoeRadius() {
        return config.getDouble("hammer.verdict-aoe-radius", 4.0);
    }

    public double getHammerVerdictKnockback() {
        return config.getDouble("hammer.verdict-knockback", 2.5);
    }

    public int getHammerDashCooldownSeconds() {
        return config.getInt("hammer.dash-cooldown", 3);
    }

    public int getHammerComboWindowTicks() {
        return config.getInt("hammer.combo-window-ticks", 60);
    }

    // ── Dragon Egg ────────────────────────────────────

    public int getEggHolderSpeedAmplifier() {
        return config.getInt("dragon-egg.holder-speed-amplifier", 1);
    }

    public int getEggHolderStrengthAmplifier() {
        return config.getInt("dragon-egg.holder-strength-amplifier", 0);
    }

    public boolean isEggHolderGlows() {
        return config.getBoolean("dragon-egg.holder-glows", true);
    }

    public boolean isAnnounceEggPickup() {
        return config.getBoolean("dragon-egg.announce-egg-pickup", true);
    }

    // ── Messages ──────────────────────────────────────

    public String getMessage(String key) {
        String prefix = config.getString("messages.prefix", "&8[&6RankedSMP&8] ");
        String msg = config.getString("messages." + key, "");
        return colorize(prefix + msg);
    }

    public String getRawMessage(String key) {
        return colorize(config.getString("messages." + key, ""));
    }

    // ── Player Data (config persistence) ─────────────

    public void saveRankToConfig(UUID uuid, int rank) {
        config.set("players." + uuid.toString(), rank);
        plugin.saveConfig();
    }

    public void removeRankFromConfig(UUID uuid) {
        config.set("players." + uuid.toString(), null);
        plugin.saveConfig();
    }

    public void clearAllRanksFromConfig() {
        config.set("players", null);
        plugin.saveConfig();
    }

    public Map<UUID, Integer> loadAllRanksFromConfig() {
        Map<UUID, Integer> ranks = new HashMap<>();
        if (!config.isConfigurationSection("players")) return ranks;
        for (String key : config.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int rank = config.getInt("players." + key);
                ranks.put(uuid, rank);
            } catch (IllegalArgumentException ignored) {}
        }
        return ranks;
    }

    // ── Utility ───────────────────────────────────────

    public static String colorize(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }
}
