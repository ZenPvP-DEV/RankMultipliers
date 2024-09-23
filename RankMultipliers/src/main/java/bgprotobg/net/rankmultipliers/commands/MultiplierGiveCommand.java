package bgprotobg.net.rankmultipliers.commands;

import bgprotobg.net.rankmultipliers.RankMultipliers;
import com.edwardbelt.edprison.events.EdPrisonAddMultiplierCurrency;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MultiplierGiveCommand implements CommandExecutor, TabCompleter {

    private final RankMultipliers plugin;

    public MultiplierGiveCommand(RankMultipliers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.hasPermission("multiplier.give"))) {
            sender.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }
        if (args.length < 5) {
            sender.sendMessage("§cUsage: /multiplier give <player> <currency> <boost amount> <time>");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }
        UUID targetUUID = targetPlayer.getUniqueId();

        String currency = args[2].toLowerCase();
        List<String> validCurrencies = Arrays.asList("tokens", "gems", "money");
        if (!validCurrencies.contains(currency)) {
            sender.sendMessage("§cInvalid currency. Valid currencies: tokens, gems, money.");
            return true;
        }

        double boostAmount;
        try {
            boostAmount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid boost amount.");
            return true;
        }

        long duration = parseDuration(args[4]);
        if (duration <= 0) {
            sender.sendMessage("§cInvalid time format. Use 1s/1m/1h/1d/1w.");
            return true;
        }

        plugin.addPlayerMultiplier(targetUUID, currency, boostAmount, duration);

        Player player = Bukkit.getPlayer(targetUUID);
        if (player != null) {
            Map<String, String> playerPlaceholders = new HashMap<>();
            playerPlaceholders.put("%currency%", currency.substring(0, 1).toUpperCase() + currency.substring(1));
            playerPlaceholders.put("%amount%", String.valueOf(boostAmount));
            playerPlaceholders.put("%time%", args[4]);
            player.sendMessage(plugin.getMessage("messages.player_received_multiplier", playerPlaceholders));
        }

        Map<String, String> senderPlaceholders = new HashMap<>();
        senderPlaceholders.put("%player%", targetPlayer.getName());
        senderPlaceholders.put("%time%", args[4]);
        sender.sendMessage(plugin.getMessage("messages.sender_applied_multiplier", senderPlaceholders));

        return true;
    }


    private long parseDuration(String time) {
        long duration = 0;
        try {
            if (time.endsWith("s")) {
                duration = Long.parseLong(time.replace("s", "")) * 1000;
            } else if (time.endsWith("m")) {
                duration = Long.parseLong(time.replace("m", "")) * 1000 * 60;
            } else if (time.endsWith("h")) {
                duration = Long.parseLong(time.replace("h", "")) * 1000 * 60 * 60;
            } else if (time.endsWith("d")) {
                duration = Long.parseLong(time.replace("d", "")) * 1000 * 60 * 60 * 24;
            } else if (time.endsWith("w")) {
                duration = Long.parseLong(time.replace("w", "")) * 1000 * 60 * 60 * 24 * 7;
            } else {
                return -1;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
        return duration;
    }




    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 2) {
            return null;
        } else if (args.length == 3) {
            return Arrays.asList("tokens", "gems", "money");
        } else if (args.length == 4) {
            return Arrays.asList("1.0", "2.0", "3.0");
        } else if (args.length == 5) {
            return Arrays.asList("10s", "1m", "1h", "1d", "1w");
        }
        return null;
    }
}
