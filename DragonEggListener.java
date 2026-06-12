package me.rankedsmp.listeners;

import me.rankedsmp.RankedSMP;
import me.rankedsmp.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DragonEggListener implements Listener {

    private final RankedSMP plugin;
    private final ConfigManager cfg;
    // Players currently holding the dragon egg
    private final Map<UUID, BukkitTask> eggHolders = new HashMap<>();

    public DragonEggListener(RankedSMP plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfigManager();
        // Periodic check for existing holders on reload
        startPeriodicCheck();
    }

    // ─────────────────────────────────────────────────
    // Egg detection on pickup / inventory actions
    // ─────────────────────────────────────────────────

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getItem().getItemStack().getType() == Material.DRAGON_EGG) {
            // Delay to let the item enter inventory
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkAndApplyEgg(player), 2L);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkAndApplyEgg(player), 2L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> checkAndApplyEgg(player), 2L);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == Material.DRAGON_EGG) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                removeEggBuffs(event.getPlayer());
            }, 2L);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                checkAndApplyEgg(event.getPlayer()), 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeEggBuffs(event.getPlayer());
    }

    // ─────────────────────────────────────────────────
    // Buff application
    // ─────────────────────────────────────────────────

    private void checkAndApplyEgg(Player player) {
        if (hasEggInInventory(player)) {
            if (!eggHolders.containsKey(player.getUniqueId())) {
                applyEggBuffs(player);
            }
        } else {
            removeEggBuffs(player);
        }
    }

    private void applyEggBuffs(Player player) {
        eggHolders.put(player.getUniqueId(), null); // Mark as holder

        // Apply glow
        if (cfg.isEggHolderGlows()) {
            player.setGlowing(true);
        }

        // Announce
        if (cfg.isAnnounceEggPickup()) {
            Bukkit.broadcastMessage(cfg.colorize(
                    "&8[&6Dragon Egg&8] &e" + player.getName() + " &7now holds the Dragon Egg! &cThey are glowing!"
            ));
        }

        // Apply buffs on a repeating task (refresh every 5s so they don't expire)
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !hasEggInInventory(player)) {
                    removeEggBuffs(player);
                    cancel();
                    return;
                }
                // Speed buff
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED,
                        120, // 6 seconds
                        cfg.getEggHolderSpeedAmplifier(),
                        false, true, true
                ));
                // Strength buff
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.STRENGTH,
                        120,
                        cfg.getEggHolderStrengthAmplifier(),
                        false, true, true
                ));
            }
        }.runTaskTimer(plugin, 0L, 80L); // Every 4 seconds

        eggHolders.put(player.getUniqueId(), task);
    }

    private void removeEggBuffs(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = eggHolders.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        if (player.isOnline()) {
            player.setGlowing(false);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.STRENGTH);
        }
    }

    private boolean hasEggInInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.DRAGON_EGG) return true;
        }
        return false;
    }

    /** Periodic sweep for any logged-in players that have the egg */
    private void startPeriodicCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    checkAndApplyEgg(p);
                }
            }
        }.runTaskTimer(plugin, 100L, 200L); // Every 10 seconds
    }
}
