package me.rankedsmp.listeners;

import me.rankedsmp.RankedSMP;
import me.rankedsmp.items.HierarchyHammer;
import me.rankedsmp.managers.ConfigManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class HierarchyHammerListener implements Listener {

    private final RankedSMP plugin;
    private final ConfigManager cfg;

    // Per-player state
    private final Map<UUID, Integer> hitCounter = new HashMap<>();       // consecutive hits
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();       // last dash time (ms)
    private final Map<UUID, Long> lastHitTime = new HashMap<>();         // last hit time (ms)
    private final Map<UUID, BukkitTask> comboResetTasks = new HashMap<>();

    public HierarchyHammerListener(RankedSMP plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
    }

    // ─────────────────────────────────────────────────
    // Right-click → Dash
    // ─────────────────────────────────────────────────

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!HierarchyHammer.isHierarchyHammer(player.getInventory().getItemInMainHand(), plugin)) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldownMs = cfg.getHammerDashCooldownSeconds() * 1000L;
        long lastDash = dashCooldowns.getOrDefault(uuid, 0L);

        if (now - lastDash < cooldownMs) {
            double remaining = (cooldownMs - (now - lastDash)) / 1000.0;
            sendActionBar(player, ChatColor.RED + "Dash cooldown: " + String.format("%.1f", remaining) + "s");
            return;
        }

        dashCooldowns.put(uuid, now);
        performDash(player);
    }

    private void performDash(Player player) {
        Vector direction = player.getLocation().getDirection().normalize();
        direction.setY(Math.max(direction.getY(), 0.15)); // Small upward component
        direction.multiply(1.8);

        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_DEFLECT, 1.0f, 1.2f);
        sendActionBar(player, ChatColor.YELLOW + "» " + ChatColor.WHITE + "Dashing!");

        // Particle trail during dash
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks > 8) { cancel(); return; }
                player.getWorld().spawnParticle(
                        Particle.DUST,
                        player.getLocation().add(0, 0.8, 0),
                        5, 0.2, 0.2, 0.2,
                        new Particle.DustOptions(Color.fromRGB(255, 200, 50), 1.2f)
                );
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    // ─────────────────────────────────────────────────
    // Attack → Combo & Verdict
    // ─────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (!HierarchyHammer.isHierarchyHammer(attacker.getInventory().getItemInMainHand(), plugin)) return;

        UUID uuid = attacker.getUniqueId();
        int hits = hitCounter.getOrDefault(uuid, 0) + 1;
        int hitsNeeded = cfg.getHammerHitsToCharge();

        // Cancel any pending miss reset
        cancelComboReset(uuid);

        if (hits >= hitsNeeded) {
            // VERDICT activated!
            executeVerdict(attacker, (LivingEntity) event.getEntity(), event);
            hitCounter.put(uuid, 0);
        } else {
            hitCounter.put(uuid, hits);
            lastHitTime.put(uuid, System.currentTimeMillis());
            updateComboBar(attacker, hits, hitsNeeded);
            // Schedule combo reset if no hit in window
            scheduleComboReset(attacker, uuid);
        }
    }

    private void executeVerdict(Player attacker, LivingEntity primary, EntityDamageByEntityEvent event) {
        double baseDamage = event.getDamage();
        double verdictDamage = baseDamage * cfg.getHammerVerdictDamageMultiplier();
        event.setDamage(verdictDamage);

        Location loc = primary.getLocation();
        World world = loc.getWorld();
        double radius = cfg.getHammerVerdictAoeRadius();

        // Visual effects
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 3, 0.5, 0.5, 0.5);
        world.spawnParticle(Particle.GUST_EMITTER_LARGE, loc, 2, 0.3, 0.3, 0.3);
        world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 1.0f, 0.6f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);

        // Shockwave ring particles
        for (double angle = 0; angle < 360; angle += 15) {
            double rad = Math.toRadians(angle);
            double x = Math.cos(rad) * radius * 0.8;
            double z = Math.sin(rad) * radius * 0.8;
            world.spawnParticle(Particle.DUST,
                    loc.clone().add(x, 0.1, z),
                    3, 0.1, 0.1, 0.1,
                    new Particle.DustOptions(Color.fromRGB(255, 120, 0), 1.5f)
            );
        }

        // AoE knockback on nearby entities
        double knockback = cfg.getHammerVerdictKnockback();
        for (Entity nearby : world.getNearbyEntities(loc, radius, radius, radius)) {
            if (nearby.equals(attacker) || nearby.equals(primary)) continue;
            if (!(nearby instanceof LivingEntity)) continue;

            Vector away = nearby.getLocation().subtract(loc).toVector().normalize();
            away.setY(0.4);
            nearby.setVelocity(away.multiply(knockback));

            if (nearby instanceof Player nearbyPlayer) {
                nearbyPlayer.damage(baseDamage * 0.5, attacker);
                sendActionBar(nearbyPlayer, ChatColor.DARK_RED + "⚠ Hit by Verdict shockwave!");
            }
        }

        // Feedback to attacker
        sendActionBar(attacker, ChatColor.GOLD + "" + ChatColor.BOLD + "⚡ VERDICT!");
        attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);

        // Set weapon cooldown (small cooldown after verdict)
        attacker.setCooldown(attacker.getInventory().getItemInMainHand().getType(), 15);
    }

    // ─────────────────────────────────────────────────
    // Combo management
    // ─────────────────────────────────────────────────

    private void scheduleComboReset(Player player, UUID uuid) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (hitCounter.getOrDefault(uuid, 0) > 0) {
                    hitCounter.put(uuid, 0);
                    if (player.isOnline()) {
                        sendActionBar(player, ChatColor.RED + "✗ Combo reset!");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);
                    }
                }
            }
        }.runTaskLater(plugin, cfg.getHammerComboWindowTicks());

        cancelComboReset(uuid);
        comboResetTasks.put(uuid, task);
    }

    private void cancelComboReset(UUID uuid) {
        BukkitTask old = comboResetTasks.remove(uuid);
        if (old != null) old.cancel();
    }

    private void updateComboBar(Player player, int hits, int needed) {
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.YELLOW).append("Combo: ");
        for (int i = 0; i < needed; i++) {
            if (i < hits) {
                bar.append(ChatColor.GOLD).append("⬛");
            } else {
                bar.append(ChatColor.DARK_GRAY).append("⬛");
            }
        }
        int remaining = needed - hits;
        bar.append(ChatColor.GRAY).append("  (").append(remaining).append(" more to charge)");
        sendActionBar(player, bar.toString());
    }

    // ─────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        hitCounter.remove(uuid);
        dashCooldowns.remove(uuid);
        lastHitTime.remove(uuid);
        cancelComboReset(uuid);
    }

    // ─────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(message));
    }
}
