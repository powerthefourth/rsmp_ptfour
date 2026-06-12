package me.rankedsmp.items;

import me.rankedsmp.RankedSMP;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class HierarchyHammer {

    private final RankedSMP plugin;
    public static final String HAMMER_KEY = "hierarchy_hammer";

    public HierarchyHammer(RankedSMP plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates the Hierarchy Hammer ItemStack with full lore and PDC marker.
     */
    public ItemStack createHammer() {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Hierarchy Hammer");

        List<String> lore = Arrays.asList(
                "",
                ChatColor.DARK_GRAY + "A powerful hammer that manifested",
                ChatColor.DARK_GRAY + "as a result of improper judgement.",
                "",
                ChatColor.YELLOW + "▶ " + ChatColor.WHITE + "Right-click" + ChatColor.GRAY + " to dash forward.",
                ChatColor.YELLOW + "▶ " + ChatColor.WHITE + "Land " + ChatColor.GOLD + plugin.getConfigManager().getHammerHitsToCharge() + " consecutive hits",
                ChatColor.GRAY + "  without missing to charge " + ChatColor.GOLD + "VERDICT" + ChatColor.GRAY + ".",
                ChatColor.YELLOW + "▶ " + ChatColor.GOLD + "VERDICT" + ChatColor.GRAY + ": Double damage + AoE shockwave.",
                "",
                ChatColor.RED + "✗ Missing an attack resets the combo.",
                ""
        );
        meta.setLore(lore);

        // PDC tag so we can identify this item reliably
        NamespacedKey key = new NamespacedKey(plugin, HAMMER_KEY);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Returns true if the given ItemStack is a Hierarchy Hammer.
     */
    public static boolean isHierarchyHammer(ItemStack item, Plugin plugin) {
        if (item == null || item.getType() != Material.MACE) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey key = new NamespacedKey(plugin, HAMMER_KEY);
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }
}
