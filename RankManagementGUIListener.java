package me.rankedsmp.listeners;

import me.rankedsmp.RankedSMP;
import me.rankedsmp.gui.RankManagementGUI;
import me.rankedsmp.managers.RankManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.ClickType;

public class RankManagementGUIListener implements Listener {

    private final RankedSMP plugin;
    private final RankManagementGUI gui;
    private final RankManager rankManager;

    public RankManagementGUIListener(RankedSMP plugin) {
        this.plugin = plugin;
        this.gui = plugin.getRankManagementGUI();
        this.rankManager = plugin.getRankManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!gui.isManagementGUI(event.getInventory())) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot >= event.getInventory().getSize()) return; // Bottom inventory click

        // Right-click in rank slots: remove rank
        if (slot < 20 && (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT)) {
            int rank = slot + 1;
            var atRank = rankManager.getPlayerAtRank(rank);
            if (atRank != null) {
                rankManager.removePlayerRank(atRank);
                player.sendMessage("§aRemoved rank #" + rank + ".");
                player.closeInventory();
                gui.openGUI(player);
            }
            return;
        }

        gui.handleClick(player, event.getInventory(), slot);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (gui.isManagementGUI(event.getInventory())) {
            gui.removeGUI(event.getInventory());
        }
    }
}
