package me.rankedsmp.listeners;

import me.rankedsmp.RankedSMP;
import me.rankedsmp.managers.ConfigManager;
import me.rankedsmp.managers.RankManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class PotionListener implements Listener {

    private final RankedSMP plugin;
    private final RankManager rankManager;
    private final ConfigManager cfg;

    private final Set<PotionEffectType> negativeEffects = new HashSet<>();

    public PotionListener(RankedSMP plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
        this.cfg = plugin.getConfigManager();
        initNegativeEffects();
    }

    private void initNegativeEffects() {
        negativeEffects.add(PotionEffectType.SLOWNESS);
        negativeEffects.add(PotionEffectType.MINING_FATIGUE);
        negativeEffects.add(PotionEffectType.NAUSEA);
        negativeEffects.add(PotionEffectType.BLINDNESS);
        negativeEffects.add(PotionEffectType.HUNGER);
        negativeEffects.add(PotionEffectType.WEAKNESS);
        negativeEffects.add(PotionEffectType.POISON);
        negativeEffects.add(PotionEffectType.WITHER);
        negativeEffects.add(PotionEffectType.LEVITATION);
        negativeEffects.add(PotionEffectType.UNLUCK);
        negativeEffects.add(PotionEffectType.DARKNESS);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        // Only process when an effect is being added
        if (event.getAction() != EntityPotionEffectEvent.Action.ADDED) return;
        // Avoid infinite loop for plugin-applied effects
        if (event.getCause() == EntityPotionEffectEvent.Cause.PLUGIN) return;
        if (!(event.getEntity() instanceof Player player)) return;

        int rank = rankManager.getPlayerRank(player);
        if (rank == -1) return; // Unranked players get no scaling

        PotionEffect newEffect = event.getNewEffect();
        if (newEffect == null) return;

        boolean isNegative = negativeEffects.contains(newEffect.getType());
        double multiplier;

        if (isNegative && cfg.isScaleNegativeEffects()) {
            // Higher rank = shorter negative effect duration (inverse scaling)
            double positiveMultiplier = cfg.getPotionMultiplierForRank(rank);
            // Negative effects: rank 1 = 0.5x duration, rank 20 = 1x duration
            multiplier = 1.5 - (positiveMultiplier - 1.0); // Inverted
            if (multiplier < 0.3) multiplier = 0.3;
        } else {
            multiplier = cfg.getPotionMultiplierForRank(rank);
        }

        int newDuration = (int) Math.round(newEffect.getDuration() * multiplier);
        // Prevent infinite effects from being scaled
        if (newEffect.getDuration() == Integer.MAX_VALUE) return;

        event.setCancelled(true);
        PotionEffect scaled = new PotionEffect(
                newEffect.getType(),
                newDuration,
                newEffect.getAmplifier(),
                newEffect.isAmbient(),
                newEffect.hasParticles(),
                newEffect.hasIcon()
        );
        // Apply via scheduler to avoid event recursion
        plugin.getServer().getScheduler().runTask(plugin, () ->
                player.addPotionEffect(scaled)
        );
    }
}
