package me.rankedsmp.managers;

import me.rankedsmp.RankedSMP;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExtraInventoryManager {

    private final RankedSMP plugin;
    private final Map<UUID, Inventory> extraInventories = new HashMap<>();
    private final Map<UUID, Integer> inventorySlots = new HashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public ExtraInventoryManager(RankedSMP plugin) {
        this.plugin = plugin;
        setupDataFile();
    }

    private void setupDataFile() {
        dataFile = new File(plugin.getDataFolder(), "extra_inventories.yml");
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create extra_inventories.yml!");
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    // ─────────────────────────────────────────────────
    // Open / Close
    // ─────────────────────────────────────────────────

    public void openExtraInventory(Player player) {
        RankManager rm = plugin.getRankManager();
        int rank = rm.getPlayerRank(player);

        if (rank == -1 || rank > 10) {
            player.sendMessage(plugin.getConfigManager().getMessage("top10-only"));
            return;
        }

        int slots = plugin.getConfigManager().getExtraInventorySlotsForRank(rank);
        if (slots <= 0) {
            player.sendMessage(plugin.getConfigManager().getMessage("top10-only"));
            return;
        }

        Inventory inv = getOrCreateInventory(player.getUniqueId(), rank, slots);
        player.openInventory(inv);
    }

    private Inventory getOrCreateInventory(UUID uuid, int rank, int slots) {
        Inventory existing = extraInventories.get(uuid);
        int oldSlots = inventorySlots.getOrDefault(uuid, 0);

        if (existing != null && oldSlots == slots) {
            return existing;
        }

        // Need to create or resize
        String title = ChatColor.GOLD + "Extra Inventory " + ChatColor.YELLOW + "[#" + rank + "]";
        Inventory newInv = Bukkit.createInventory(null, slots, title);

        // Transfer old items if resizing
        if (existing != null) {
            ItemStack[] old = existing.getContents();
            for (int i = 0; i < Math.min(old.length, slots); i++) {
                newInv.setItem(i, old[i]);
            }
            // Drop overflow items
            if (old.length > slots) {
                Player p = Bukkit.getPlayer(uuid);
                for (int i = slots; i < old.length; i++) {
                    if (old[i] != null) {
                        if (p != null) p.getInventory().addItem(old[i]);
                    }
                }
            }
        } else {
            // Try loading from save
            loadInventoryFromFile(uuid, newInv);
        }

        extraInventories.put(uuid, newInv);
        inventorySlots.put(uuid, slots);
        return newInv;
    }

    // ─────────────────────────────────────────────────
    // Rank change handler
    // ─────────────────────────────────────────────────

    public void onRankChange(Player player, int oldRank, int newRank) {
        int newSlots = plugin.getConfigManager().getExtraInventorySlotsForRank(newRank);

        if (newRank > 10 || newSlots == 0) {
            // Lost extra inventory access - save and close
            if (extraInventories.containsKey(player.getUniqueId())) {
                saveInventoryToFile(player.getUniqueId());
                Inventory open = player.getOpenInventory().getTopInventory();
                if (isExtraInventory(open)) {
                    player.closeInventory();
                    player.sendMessage(ChatColor.RED + "Your extra inventory access has been revoked.");
                }
            }
            return;
        }

        // Resize if slot count changed
        int oldSlots = inventorySlots.getOrDefault(player.getUniqueId(), 0);
        if (newSlots != oldSlots) {
            getOrCreateInventory(player.getUniqueId(), newRank, newSlots);
        }
    }

    // ─────────────────────────────────────────────────
    // Identification
    // ─────────────────────────────────────────────────

    public boolean isExtraInventory(Inventory inventory) {
        if (inventory == null) return false;
        return extraInventories.containsValue(inventory);
    }

    // ─────────────────────────────────────────────────
    // Save / Load
    // ─────────────────────────────────────────────────

    private void saveInventoryToFile(UUID uuid) {
        Inventory inv = extraInventories.get(uuid);
        if (inv == null) return;
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i] != null) {
                dataConfig.set(uuid + ".slot" + i, contents[i]);
            } else {
                dataConfig.set(uuid + ".slot" + i, null);
            }
        }
        dataConfig.set(uuid + ".size", contents.length);
        saveDataFile();
    }

    private void loadInventoryFromFile(UUID uuid, Inventory inv) {
        if (!dataConfig.contains(uuid.toString())) return;
        int size = dataConfig.getInt(uuid + ".size", inv.getSize());
        for (int i = 0; i < Math.min(size, inv.getSize()); i++) {
            ItemStack item = dataConfig.getItemStack(uuid + ".slot" + i);
            if (item != null) {
                inv.setItem(i, item);
            }
        }
    }

    public void saveAllInventories() {
        for (UUID uuid : extraInventories.keySet()) {
            saveInventoryToFile(uuid);
        }
    }

    private void saveDataFile() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save extra_inventories.yml: " + e.getMessage());
        }
    }

    public void savePlayerInventory(UUID uuid) {
        saveInventoryToFile(uuid);
    }

    public Inventory getExtraInventory(UUID uuid) {
        return extraInventories.get(uuid);
    }
}
