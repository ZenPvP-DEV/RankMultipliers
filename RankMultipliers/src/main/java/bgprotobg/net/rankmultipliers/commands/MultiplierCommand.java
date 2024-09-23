package bgprotobg.net.rankmultipliers.commands;

import bgprotobg.net.rankmultipliers.RankMultipliers;
import com.edwardbelt.edprison.events.EdPrisonAddMultiplierCurrency;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MultiplierCommand implements CommandExecutor {

    private final RankMultipliers plugin;
    private final MultiplierGiveCommand giveCommand;

    public MultiplierCommand(RankMultipliers plugin) {
        this.plugin = plugin;
        this.giveCommand = new MultiplierGiveCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!(sender instanceof Player) || sender.hasPermission("multiplier.reload")) {
                    plugin.reloadConfig();
                    sender.sendMessage("§aConfiguration reloaded successfully.");
                } else {
                    sender.sendMessage("§cYou don't have permission to reload the configuration.");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("give")) {
                return giveCommand.onCommand(sender, command, label, args);
            } else if (args[0].equalsIgnoreCase("active")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    if (player.hasPermission("multiplier.use")) {
                        showActiveMultipliers(player);
                    } else {
                        player.sendMessage("§cYou don't have permission to use this command.");
                    }
                } else {
                    sender.sendMessage("§cOnly players can use this command.");
                }
                return true;
            }
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (player.hasPermission("multiplier.use")) {
                openMultiplierMenu(player);
            } else {
                player.sendMessage("§cYou don't have permission to use this command.");
            }
        } else {
            sender.sendMessage("§cOnly players can use this command.");
        }
        return true;
    }

    private void showActiveMultipliers(Player player) {
        UUID playerUUID = player.getUniqueId();
        Map<String, Double> multipliers = plugin.getPlayerMultipliers(playerUUID);
        Map<String, Long> expiryTimes = plugin.getPlayerMultiplierExpiry(playerUUID);

        if (multipliers == null || multipliers.isEmpty()) {
            player.sendMessage(plugin.getMessage("messages.no_active_multipliers"));
            return;
        }

        player.sendMessage(plugin.getMessage("messages.active_multipliers_title"));
        for (String currency : multipliers.keySet()) {
            double amount = multipliers.get(currency);
            long expiryTime = expiryTimes.get(currency);
            long timeLeft = expiryTime - System.currentTimeMillis();

            String formattedTimeLeft = formatTimeLeft(timeLeft);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%currency%", currency.substring(0, 1).toUpperCase() + currency.substring(1));
            placeholders.put("%amount%", String.valueOf(amount));
            placeholders.put("%time_left%", formattedTimeLeft);

            player.sendMessage(plugin.getMessage("messages.active_multiplier_format", placeholders));
        }
    }

    private String formatTimeLeft(long timeLeftMillis) {
        if (timeLeftMillis <= 0) {
            return plugin.getConfig()
                    .getString("messages.no_boost", "§cNo Boost Activated")
                    .replace("&", "§");
        }


        long seconds = timeLeftMillis / 1000 % 60;
        long minutes = timeLeftMillis / 1000 / 60 % 60;
        long hours = timeLeftMillis / 1000 / 60 / 60 % 24;
        long days = timeLeftMillis / 1000 / 60 / 60 / 24 % 7;
        long weeks = timeLeftMillis / 1000 / 60 / 60 / 24 / 7;

        StringBuilder formattedTime = new StringBuilder();
        if (weeks > 0) formattedTime.append(weeks).append("w ");
        if (days > 0 || weeks > 0) formattedTime.append(days).append("d ");
        if (hours > 0 || days > 0 || weeks > 0) formattedTime.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0 || weeks > 0) formattedTime.append(minutes).append("m ");
        formattedTime.append(seconds).append("s");

        return formattedTime.toString().trim();
    }

    private void openMultiplierMenu(Player player) {
        FileConfiguration config = plugin.getConfig();

        int size = config.getInt("menu.inventory.size", 27);
        String title = config.getString("menu.inventory.title", "§aYour Multipliers").replace("&", "§");

        Inventory inv = Bukkit.createInventory(null, size, title);

        Material fillerMaterial = Material.getMaterial(config.getString("menu.inventory.filler.material", "GRAY_STAINED_GLASS_PANE"));
        if (fillerMaterial == null) {
            fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
        }

        ItemStack filler = new ItemStack(fillerMaterial);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }

        UUID playerUUID = player.getUniqueId();

        Map<String, Double> personalMultipliers = plugin.getPlayerMultipliers(playerUUID);
        if (personalMultipliers == null) {
            personalMultipliers = new HashMap<>();
        }

        Map<String, Long> personalExpiryTimes = plugin.getPlayerMultiplierExpiry(playerUUID);
        if (personalExpiryTimes == null) {
            personalExpiryTimes = new HashMap<>();
        }

        createPersonalMultiplierItem(inv, player, personalMultipliers, personalExpiryTimes);

        Material headMaterial = Material.getMaterial(config.getString("menu.head.material", "PLAYER_HEAD"));
        int headSlot = config.getInt("menu.head.slot", 14);
        ItemStack head = new ItemStack(headMaterial);

        if (headMaterial == Material.PLAYER_HEAD) {
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(player);
                skullMeta.setDisplayName(config.getString("menu.head.name").replace("&", "§").replace("%player_name%", player.getName()));
                List<String> lore = config.getStringList("menu.head.lore").stream()
                        .map(line -> PlaceholderAPI.setPlaceholders(player, line.replace("&", "§")))
                        .collect(Collectors.toList());
                skullMeta.setLore(lore);
                head.setItemMeta(skullMeta);
            }
        } else {
            ItemMeta itemMeta = head.getItemMeta();
            if (itemMeta != null) {
                itemMeta.setDisplayName(config.getString("menu.head.name").replace("&", "§").replace("%player_name%", player.getName()));
                List<String> lore = config.getStringList("menu.head.lore").stream()
                        .map(line -> PlaceholderAPI.setPlaceholders(player, line.replace("&", "§")))
                        .collect(Collectors.toList());
                itemMeta.setLore(lore);
                head.setItemMeta(itemMeta);
            }
        }

        inv.setItem(headSlot, head);
        player.openInventory(inv);
    }




    private void createPersonalMultiplierItem(Inventory inv, Player player, Map<String, Double> multipliers, Map<String, Long> expiryTimes) {
        FileConfiguration config = plugin.getConfig();

        Material headMaterial = Material.getMaterial(config.getString("personal.head.material", "PLAYER_HEAD"));
        int slot = config.getInt("personal.head.slot", 12);
        ItemStack head = new ItemStack(headMaterial);
        ItemMeta itemMeta = head.getItemMeta();

        if (headMaterial == Material.PLAYER_HEAD && itemMeta instanceof SkullMeta) {
            ((SkullMeta) itemMeta).setOwningPlayer(player);
        }

        String displayName = config.getString("personal.head.name")
                .replace("&", "§")
                .replace("%player_name%", player.getName());
        itemMeta.setDisplayName(displayName);

        List<String> lore = config.getStringList("personal.head.lore").stream()
                .map(line -> line.replace("&", "§")
                        .replace("%tokens_multiplier%", String.valueOf(multipliers.getOrDefault("tokens", 1.0)))
                        .replace("%tokens_time_left%", formatTimeLeft(expiryTimes.getOrDefault("tokens", 0L) - System.currentTimeMillis()))
                        .replace("%gems_multiplier%", String.valueOf(multipliers.getOrDefault("gems", 1.0)))
                        .replace("%gems_time_left%", formatTimeLeft(expiryTimes.getOrDefault("gems", 0L) - System.currentTimeMillis()))
                        .replace("%money_multiplier%", String.valueOf(multipliers.getOrDefault("money", 1.0)))
                        .replace("%money_time_left%", formatTimeLeft(expiryTimes.getOrDefault("money", 0L) - System.currentTimeMillis())))
                .collect(Collectors.toList());

        itemMeta.setLore(lore);
        head.setItemMeta(itemMeta);

        inv.setItem(slot, head);
    }

}
