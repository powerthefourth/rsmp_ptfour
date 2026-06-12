package me.rankedsmp.gui;

import me.rankedsmp.RankedSMP;
import me.rankedsmp.managers.RankManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class RankManagementGUI {

    private final RankedSMP plugin;
    private final RankManager rankManager;

    // Tracks which inventories are management GUIs
    private final Set<Inventory> openGUIs = new HashSet<>();

    // Track selected player for swap
    private final Map<UUID, UUID> selectedForMove = new HashMap<>();

    public RankManagementGUI(RankedSMP plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
    }

    // ─────────────────────────────────────────────────
    // Open
    // ─────────────────────────────────────────────────

    public void openGUI(Player admin) {
        List<Map.Entry<UUID, Integer>> ranked = rankManager.getRankedPlayersSorted();

        // 54-slot GUI (6 rows) — 20 ranked players + unranked list + controls
        Inventory gui = Bukkit.createInventory(null, 54,
                ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Rank Management");

        // Fill with gray glass pane separators first
        ItemStack pane = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 36; i < 54; i++) gui.setItem(i, pane);

        // Place ranked players (slots 0–19)
        for (Map.Entry<UUID, Integer> entry : ranked) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            int slot = entry.getValue() - 1; // Rank 1 = slot 0
            if (slot >= 0 && slot < 20) {
                gui.setItem(slot, createPlayerHead(op, entry.getValue()));
            }
        }

        // Fill empty rank slots with placeholder
        for (int i = 0; i < 20; i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, makeItem(Material.BARRIER,
                        ChatColor.RED + "[Empty - Rank #" + (i + 1) + "]",
                        Arrays.asList(ChatColor.GRAY + "No player assigned")));
            }
        }

        // Control buttons row (bottom)
        gui.setItem(45, makeItem(Material.LIME_WOOL, ChatColor.GREEN + "Start RankedSMP",
                Arrays.asList(ChatColor.GRAY + "Assign ranks to online players")));
        gui.setItem(46, makeItem(Material.RED_WOOL, ChatColor.RED + "Stop RankedSMP",
                Arrays.asList(ChatColor.GRAY + "Clear all ranks")));
        gui.setItem(47, makeItem(Material.PAPER, ChatColor.YELLOW + "Reload Config",
                Arrays.asList(ChatColor.GRAY + "Reload plugin configuration")));
        gui.setItem(49, makeItem(Material.BOOK, ChatColor.GOLD + "Rank List",
                getRankListLore(ranked)));
        gui.setItem(53, makeItem(Material.ARROW, ChatColor.WHITE + "Close", null));

        openGUIs.add(gui);
        admin.openInventory(gui);
    }

    private List<String> getRankListLore(List<Map.Entry<UUID, Integer>> ranked) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current Rankings:");
        lore.add("");
        for (Map.Entry<UUID, Integer> e : ranked) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(e.getKey());
            String name = op.getName() != null ? op.getName() : "Unknown";
            boolean online = op.isOnline();
            lore.add((online ? ChatColor.GREEN : ChatColor.GRAY) + "#" + e.getValue() + " - " + name);
        }
        if (ranked.isEmpty()) lore.add(ChatColor.RED + "No ranked players");
        return lore;
    }

    // ─────────────────────────────────────────────────
    // Handle Clicks
    // ─────────────────────────────────────────────────

    public void handleClick(Player admin, Inventory gui, int slot) {
        // Bottom row controls
        if (slot == 45) {
            admin.closeInventory();
            rankManager.startRankedSMP(admin);
            return;
        }
        if (slot == 46) {
            admin.closeInventory();
            rankManager.stopRankedSMP();
            return;
        }
        if (slot == 47) {
            plugin.getConfigManager().reload();
            admin.sendMessage(ChatColor.GREEN + "Config reloaded.");
            admin.closeInventory();
            return;
        }
        if (slot == 53) {
            admin.closeInventory();
            return;
        }

        // Rank slots 0–19: click to remove or move rank
        if (slot < 20) {
            ItemStack clicked = gui.getItem(slot);
            if (clicked == null || clicked.getType() == Material.BARRIER) return;

            // Get the player at this rank slot
            int rank = slot + 1;
            UUID atRank = rankManager.getPlayerAtRank(rank);
            if (atRank == null) return;

            OfflinePlayer target = Bukkit.getOfflinePlayer(atRank);
            String name = target.getName() != null ? target.getName() : "Unknown";

            UUID selectedUUID = selectedForMove.get(admin.getUniqueId());
            if (selectedUUID == null) {
                // First click: select this player for move
                selectedForMove.put(admin.getUniqueId(), atRank);
                admin.sendMessage(ChatColor.YELLOW + "Selected " + name + " (#" + rank + "). Click another player to swap, or click same to deselect.");
            } else if (selectedUUID.equals(atRank)) {
                // Deselect
                selectedForMove.remove(admin.getUniqueId());
                admin.sendMessage(ChatColor.GRAY + "Deselected " + name + ".");
            } else {
                // Swap the two selected players' ranks
                int rankA = rankManager.getPlayerRank(selectedUUID);
                int rankB = rank;
                rankManager.setPlayerRank(selectedUUID, rankB);
                rankManager.setPlayerRank(atRank, rankA);
                selectedForMove.remove(admin.getUniqueId());
                admin.sendMessage(ChatColor.GREEN + "Swapped ranks: " +
                        Bukkit.getOfflinePlayer(selectedUUID).getName() + " (#" + rankB + ") ↔ " +
                        name + " (#" + rankA + ")");
                admin.closeInventory();
                openGUI(admin); // Refresh
            }
        }
    }

    // ─────────────────────────────────────────────────
    // GUI identification
    // ─────────────────────────────────────────────────

    public boolean isManagementGUI(Inventory inv) {
        return openGUIs.contains(inv);
    }

    public void removeGUI(Inventory inv) {
        openGUIs.remove(inv);
    }

    // ─────────────────────────────────────────────────
    // Item builders
    // ─────────────────────────────────────────────────

    private ItemStack createPlayerHead(OfflinePlayer op, int rank) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        meta.setOwningPlayer(op);
        String name = op.getName() != null ? op.getName() : "Unknown";
        boolean online = op.isOnline();

        meta.setDisplayName(ChatColor.GOLD + "#" + rank + " - " + ChatColor.WHITE + name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Status: " + (online ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline"));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Left-click: Select to swap");
        lore.add(ChatColor.RED + "Right-click: Remove rank");
        meta.setLore(lore);

        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack makeItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
