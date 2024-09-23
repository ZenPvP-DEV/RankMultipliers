package bgprotobg.net.rankmultipliers;

import bgprotobg.net.rankmultipliers.commands.MultiplierCommand;
import bgprotobg.net.rankmultipliers.commands.MultiplierGiveCommand;
import bgprotobg.net.rankmultipliers.listeners.MultiplierListener;
import bgprotobg.net.rankmultipliers.listeners.MultiplierMenuListener;
import bgprotobg.net.rankmultipliers.placeholders.MultiplierPlaceholders;
import bgprotobg.net.rankmultipliers.storage.DatabaseManager;
import bgprotobg.net.rankmultipliers.storage.MultiplierData;
import bgprotobg.net.rankmultipliers.listeners.PlayerJoinListener;
import com.edwardbelt.edprison.events.EdPrisonAddMultiplierCurrency;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class RankMultipliers extends JavaPlugin {

    private LuckPerms luckPerms;
    private final Map<UUID, Map<String, Double>> playerMultipliers = new HashMap<>();
    private final Map<UUID, Map<String, Long>> playerMultiplierExpiry = new HashMap<>();
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        databaseManager = new DatabaseManager(this);

        getServer().getPluginManager().registerEvents(new MultiplierListener(this), this);
        getServer().getPluginManager().registerEvents(new MultiplierMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        getCommand("multiplier").setExecutor(new MultiplierCommand(this));
        getCommand("multiplier").setTabCompleter(new MultiplierGiveCommand(this));

        luckPerms = LuckPermsProvider.get();
        getLogger().info("LuckPerms API loaded successfully.");

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MultiplierPlaceholders(this).register();
            getLogger().info("PlaceholderAPI placeholders registered successfully.");
        } else {
            getLogger().warning("PlaceholderAPI not found, placeholders will not work.");
        }
    }


    @Override
    public void onDisable() {
        for (UUID playerUUID : playerMultipliers.keySet()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                savePlayerMultipliers(player);
            }
        }
        databaseManager.close();
    }


    public void addPlayerMultiplier(UUID playerUUID, String currency, double amount, long additionalTime) {
        double existingBoost = playerMultipliers.getOrDefault(playerUUID, new HashMap<>()).getOrDefault(currency, 0.0);
        long existingExpiry = playerMultiplierExpiry.getOrDefault(playerUUID, new HashMap<>()).getOrDefault(currency, 0L);

        double newBoost = existingBoost + amount;

        long currentTime = System.currentTimeMillis();
        long newExpiryTime;

        if (existingExpiry > currentTime) {
            newExpiryTime = existingExpiry + additionalTime;
        } else {
            newExpiryTime = currentTime + additionalTime;
        }

        double maxBoost = getMaxBoost(currency);
        if (newBoost > maxBoost) {
            newBoost = maxBoost;
        }

        playerMultipliers.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(currency, newBoost);
        playerMultiplierExpiry.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(currency, newExpiryTime);

        Bukkit.getScheduler().cancelTasks(this);
        long remainingTimeMillis = newExpiryTime - System.currentTimeMillis();
        if (remainingTimeMillis > 0) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                removePlayerMultiplier(playerUUID, currency);
                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("%currency%", currency.substring(0, 1).toUpperCase() + currency.substring(1));
                    player.sendMessage(getMessage("messages.multiplier_expired", placeholders));
                }
            }, remainingTimeMillis / 50);

            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                EdPrisonAddMultiplierCurrency event = new EdPrisonAddMultiplierCurrency(playerUUID, currency, newBoost);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    savePlayerMultipliers(player);
                }
            }
        } else {
            getLogger().warning("Tried to apply an expired multiplier to " + Bukkit.getPlayer(playerUUID).getName());
        }
    }


    public void removePlayerMultiplier(UUID playerUUID, String currency) {
        Map<String, Double> multipliers = playerMultipliers.get(playerUUID);
        Map<String, Long> expiryTimes = playerMultiplierExpiry.get(playerUUID);

        if (multipliers != null) {
            multipliers.remove(currency);
        }

        if (expiryTimes != null) {
            expiryTimes.remove(currency);
        }

        Player player = Bukkit.getPlayer(playerUUID);

        if (player != null) {
            databaseManager.removeMultiplier(player.getName(), currency);
        } else {
            getLogger().warning("Attempted to remove multiplier for an offline player (UUID: " + playerUUID + ", currency: " + currency + ").");
        }
    }


    public void loadPlayerMultipliers(UUID playerUUID, String playerName) {
        databaseManager.loadMultipliers(playerName).thenAccept(multipliers -> {
            for (MultiplierData data : multipliers) {
                long expiryTime = data.getDurationLeft();
                long currentTime = System.currentTimeMillis();
                long remainingTime = expiryTime - currentTime;

                if (remainingTime > 0) {
                    playerMultipliers.computeIfAbsent(playerUUID, k -> new HashMap<>())
                            .put(data.getCurrency(), data.getAmount());
                    playerMultiplierExpiry.computeIfAbsent(playerUUID, k -> new HashMap<>())
                            .put(data.getCurrency(), expiryTime);
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        removePlayerMultiplier(playerUUID, data.getCurrency());
                        Player player = Bukkit.getPlayer(playerUUID);
                        if (player != null) {
                            Map<String, String> placeholders = new HashMap<>();
                            placeholders.put("%currency%",
                                    data.getCurrency().substring(0, 1).toUpperCase() + data.getCurrency().substring(1));
                            player.sendMessage(getMessage("messages.multiplier_expired", placeholders));
                        }
                    }, remainingTime / 50);
                } else {
                    removePlayerMultiplier(playerUUID, data.getCurrency());
                    getLogger().info("Removed expired multiplier: " + data.getCurrency() + " for player " + playerName);
                }
            }
        }).exceptionally(throwable -> {
            getLogger().severe("Failed to load multipliers for player " + playerName + ": " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        });
    }




    public void savePlayerMultipliers(Player player) {
        UUID playerUUID = player.getUniqueId();
        Map<String, Double> multipliers = playerMultipliers.get(playerUUID);
        Map<String, Long> expiryTimes = playerMultiplierExpiry.get(playerUUID);

        if (multipliers != null && expiryTimes != null) {
            for (String currency : multipliers.keySet()) {
                double amount = multipliers.get(currency);
                long expiryTime = expiryTimes.get(currency);

                long timeLeft = expiryTime - System.currentTimeMillis();

                if (timeLeft > 0) {
                    databaseManager.saveMultiplier(new MultiplierData(player.getName(), currency, amount, expiryTime));
                }
            }
        }
    }


    public double getMaxBoost(String currency) {
        return getConfig().getDouble("max-boost." + currency, Double.MAX_VALUE);
    }

    public String getMessage(String path) {
        return getConfig().getString(path).replace("&", "§");
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getConfig().getString(path).replace("&", "§");
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            message = message.replace(placeholder.getKey(), placeholder.getValue());
        }
        return message;
    }
    public Map<String, Double> getPlayerMultipliers(UUID playerUUID) {
        return playerMultipliers.get(playerUUID);
    }

    public Map<String, Long> getPlayerMultiplierExpiry(UUID playerUUID) {
        return playerMultiplierExpiry.get(playerUUID);
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
