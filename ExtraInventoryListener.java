package me.rankedsmp.listeners;

import me.rankedsmp.RankedSMP;
import me.rankedsmp.managers.ExtraInventoryManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class ExtraInventoryListener implements Listener {

    private final RankedSMP plugin;
    private final ExtraInventoryManager invManager;

    public ExtraInventoryListener(RankedSMP plugin) {
        this.plugin = plugin;
        this.invManager = plugin.getExtraInventoryManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!invManager.isExtraInventory(event.getClickedInventory())) return;
        // Allow all normal clicking in the extra inventory - no restrictions
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // Allow dragging in extra inventory
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (invManager.isExtraInventory(event.getInventory())) {
            // Save on close
            invManager.savePlayerInventory(player.getUniqueId());
        }
    }
}
