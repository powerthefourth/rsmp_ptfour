package me.rankedsmp;

import me.rankedsmp.commands.ExtraInventoryCommand;
import me.rankedsmp.commands.RankedSMPCommand;
import me.rankedsmp.gui.RankManagementGUI;
import me.rankedsmp.items.HierarchyHammer;
import me.rankedsmp.listeners.*;
import me.rankedsmp.managers.ConfigManager;
import me.rankedsmp.managers.ExtraInventoryManager;
import me.rankedsmp.managers.RankManager;
import org.bukkit.plugin.java.JavaPlugin;

public class RankedSMP extends JavaPlugin {

    private static RankedSMP instance;
    private ConfigManager configManager;
    private RankManager rankManager;
    private ExtraInventoryManager extraInventoryManager;
    private RankManagementGUI rankManagementGUI;
    private HierarchyHammer hierarchyHammer;

    @Override
    public void onEnable() {
        instance = this;

        printBanner();

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.rankManager = new RankManager(this);
        this.extraInventoryManager = new ExtraInventoryManager(this);
        this.hierarchyHammer = new HierarchyHammer(this);
        this.rankManagementGUI = new RankManagementGUI(this);

        // Load saved ranks
        rankManager.loadRanks();

        // Register listeners
        registerListeners();

        // Register commands
        registerCommands();

        getLogger().info("RankedSMP v" + getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        if (extraInventoryManager != null) {
            extraInventoryManager.saveAllInventories();
        }
        if (rankManager != null) {
            rankManager.saveRanks();
        }
        getLogger().info("RankedSMP has been disabled!");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new PvPListener(this), this);
        getServer().getPluginManager().registerEvents(new HierarchyHammerListener(this), this);
        getServer().getPluginManager().registerEvents(new ExtraInventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new PotionListener(this), this);
        getServer().getPluginManager().registerEvents(new XPListener(this), this);
        getServer().getPluginManager().registerEvents(new DragonEggListener(this), this);
        getServer().getPluginManager().registerEvents(new RankManagementGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    private void registerCommands() {
        RankedSMPCommand cmd = new RankedSMPCommand(this);
        getCommand("rankedsmp").setExecutor(cmd);
        getCommand("rankedsmp").setTabCompleter(cmd);

        ExtraInventoryCommand einv = new ExtraInventoryCommand(this);
        getCommand("extrainventory").setExecutor(einv);
    }

    private void printBanner() {
        getLogger().info("╔══════════════════════════════════╗");
        getLogger().info("║      RankedSMP v3.0 Loading      ║");
        getLogger().info("║  Inspired by SEA4's Ranked SMP   ║");
        getLogger().info("╚══════════════════════════════════╝");
    }

    // ── Getters ──────────────────────────────────────

    public static RankedSMP getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public RankManager getRankManager() { return rankManager; }
    public ExtraInventoryManager getExtraInventoryManager() { return extraInventoryManager; }
    public RankManagementGUI getRankManagementGUI() { return rankManagementGUI; }
    public HierarchyHammer getHierarchyHammer() { return hierarchyHammer; }
}
