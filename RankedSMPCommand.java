package me.rankedsmp.commands;

import me.rankedsmp.RankedSMP;
import me.rankedsmp.items.HierarchyHammer;
import me.rankedsmp.managers.RankManager;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class RankedSMPCommand implements CommandExecutor, TabCompleter {

    private final RankedSMP plugin;
    private final RankManager rankManager;

    public RankedSMPCommand(RankedSMP plugin) {
        this.plugin = plugin;
        this.rankManager = plugin.getRankManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("rankedsmp.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "help" -> sendHelp(sender);

            case "start" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(colorize("&cThis command must be run by a player."));
                    return true;
                }
                rankManager.startRankedSMP(player);
            }

            case "stop" -> {
                rankManager.stopRankedSMP();
                sender.sendMessage(colorize("&cRankedSMP stopped."));
            }

            case "reload" -> {
                plugin.getConfigManager().reload();
                sender.sendMessage(colorize("&aConfiguration reloaded."));
            }

            case "manage" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(colorize("&cMust be a player."));
                    return true;
                }
                plugin.getRankManagementGUI().openGUI(player);
            }

            case "list" -> {
                sendRankList(sender);
            }

            case "rank" -> {
                // /rsmp rank <player> <1-20|remove>
                if (args.length < 3) {
                    sender.sendMessage(colorize("&eUsage: /rsmp rank <player> <1-20|remove>"));
                    return true;
                }
                handleRankCommand(sender, args[1], args[2]);
            }

            case "give" -> {
                // /rsmp give <hammer|egg> [player]
                if (args.length < 2) {
                    sender.sendMessage(colorize("&eUsage: /rsmp give <hammer|egg> [player]"));
                    return true;
                }
                handleGiveCommand(sender, args);
            }

            case "reset" -> {
                rankManager.stopRankedSMP();
                sender.sendMessage(colorize("&cAll ranks have been reset."));
            }

            case "version" -> {
                sender.sendMessage(colorize("&6RankedSMP &av" + plugin.getDescription().getVersion()));
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    // ─────────────────────────────────────────────────
    // Sub-command handlers
    // ─────────────────────────────────────────────────

    private void handleRankCommand(CommandSender sender, String playerName, String rankArg) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(colorize("&cPlayer not found: " + playerName));
            return;
        }

        if (rankArg.equalsIgnoreCase("remove")) {
            rankManager.removePlayerRank(target.getUniqueId());
            sender.sendMessage(colorize("&aRemoved rank from " + playerName + "."));
            return;
        }

        try {
            int rank = Integer.parseInt(rankArg);
            if (rank < 1 || rank > plugin.getConfigManager().getMaxRankedPlayers()) {
                sender.sendMessage(colorize("&cRank must be between 1 and " +
                        plugin.getConfigManager().getMaxRankedPlayers() + "."));
                return;
            }
            rankManager.setPlayerRank(target.getUniqueId(), rank);
            sender.sendMessage(colorize("&aSet " + playerName + " to rank &6#" + rank + "&a."));
        } catch (NumberFormatException e) {
            sender.sendMessage(colorize("&cInvalid rank: " + rankArg));
        }
    }

    private void handleGiveCommand(CommandSender sender, String[] args) {
        String item = args[1].toLowerCase();
        Player target;

        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(colorize("&cPlayer not found: " + args[2]));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(colorize("&cSpecify a player when running from console."));
            return;
        }

        switch (item) {
            case "hammer" -> {
                target.getInventory().addItem(plugin.getHierarchyHammer().createHammer());
                sender.sendMessage(colorize("&aGave Hierarchy Hammer to " + target.getName() + "."));
            }
            case "egg" -> {
                target.getInventory().addItem(new org.bukkit.inventory.ItemStack(Material.DRAGON_EGG));
                sender.sendMessage(colorize("&aGave Dragon Egg to " + target.getName() + "."));
            }
            default -> sender.sendMessage(colorize("&cUnknown item. Use: hammer, egg"));
        }
    }

    private void sendRankList(CommandSender sender) {
        List<Map.Entry<UUID, Integer>> ranked = rankManager.getRankedPlayersSorted();
        if (ranked.isEmpty()) {
            sender.sendMessage(colorize("&cNo ranked players. Use /rsmp start to begin."));
            return;
        }
        sender.sendMessage(colorize("&6&l═══ Ranked Players ═══"));
        for (Map.Entry<UUID, Integer> entry : ranked) {
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.getKey());
            String name = op.getName() != null ? op.getName() : entry.getKey().toString().substring(0, 8);
            boolean online = op.isOnline();
            String color = entry.getValue() <= 5 ? "&e" : entry.getValue() <= 10 ? "&a" : "&b";
            sender.sendMessage(colorize(color + "#" + entry.getValue() + " &f- " + name +
                    (online ? " &a(online)" : " &7(offline)")));
        }
        sender.sendMessage(colorize("&7Total: " + ranked.size() + "/" +
                plugin.getConfigManager().getMaxRankedPlayers() + " ranked players."));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&6&l═══ RankedSMP Commands ═══"));
        sender.sendMessage(colorize("&e/rsmp start &7- Assign ranks to online players"));
        sender.sendMessage(colorize("&e/rsmp stop &7- Stop the system & clear ranks"));
        sender.sendMessage(colorize("&e/rsmp manage &7- Open rank management GUI"));
        sender.sendMessage(colorize("&e/rsmp list &7- Show all ranked players"));
        sender.sendMessage(colorize("&e/rsmp rank <player> <1-20|remove> &7- Set/remove a rank"));
        sender.sendMessage(colorize("&e/rsmp give <hammer|egg> [player] &7- Give special item"));
        sender.sendMessage(colorize("&e/rsmp reload &7- Reload config"));
        sender.sendMessage(colorize("&e/rsmp reset &7- Clear all ranks"));
        sender.sendMessage(colorize("&7Alias: /rsmp"));
    }

    // ─────────────────────────────────────────────────
    // Tab Completion
    // ─────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("rankedsmp.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return Arrays.asList("start", "stop", "reload", "manage", "list", "rank", "give", "reset", "version", "help")
                    .stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("rank") || args[0].equalsIgnoreCase("give")) {
                if (args[0].equalsIgnoreCase("give")) {
                    return Arrays.asList("hammer", "egg").stream()
                            .filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                }
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("rank")) {
            List<String> options = new ArrayList<>(Arrays.asList("remove"));
            for (int i = 1; i <= plugin.getConfigManager().getMaxRankedPlayers(); i++) {
                options.add(String.valueOf(i));
            }
            return options.stream().filter(s -> s.startsWith(args[2])).collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    // ─────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────

    private String colorize(String s) {
        return s.replace("&", "§");
    }
}
