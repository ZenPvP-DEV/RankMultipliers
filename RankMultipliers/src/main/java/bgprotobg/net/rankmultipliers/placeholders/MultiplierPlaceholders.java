package bgprotobg.net.rankmultipliers.placeholders;

import bgprotobg.net.rankmultipliers.RankMultipliers;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MultiplierPlaceholders extends PlaceholderExpansion {

    private final RankMultipliers plugin;

    public MultiplierPlaceholders(RankMultipliers plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "multiplier";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        if (identifier.equals("tokens_player")) {
            return String.format("%.2f", getTokensMultiplier(player));
        }

        if (identifier.equals("gems_player")) {
            return String.format("%.2f", getGemsMultiplier(player));
        }

        if (identifier.equals("money_player")) {
            return String.format("%.2f", getMoneyMultiplier(player));
        }

        if (identifier.equals("tokens_personal_player")) {
            return String.format("%.2f", getPersonalMultiplier(player, "tokens"));
        }

        if (identifier.equals("gems_personal_player")) {
            return String.format("%.2f", getPersonalMultiplier(player, "gems"));
        }

        if (identifier.equals("money_personal_player")) {
            return String.format("%.2f", getPersonalMultiplier(player, "money"));
        }

        if (identifier.equals("tokens_time_left")) {
            return formatTimeLeft(getTimeLeft(player, "tokens"));
        }

        if (identifier.equals("gems_time_left")) {
            return formatTimeLeft(getTimeLeft(player, "gems"));
        }

        if (identifier.equals("money_time_left")) {
            return formatTimeLeft(getTimeLeft(player, "money"));
        }

        return null;
    }

    private double getTokensMultiplier(Player player) {
        return getPlayerMultiplier(player, "tokens");
    }

    private double getGemsMultiplier(Player player) {
        return getPlayerMultiplier(player, "gems");
    }

    private double getMoneyMultiplier(Player player) {
        return getPlayerMultiplier(player, "money");
    }

    private double getPlayerMultiplier(Player player, String currency) {
        UUID playerUUID = player.getUniqueId();
        User user = plugin.getLuckPerms().getUserManager().getUser(playerUUID);
        if (user == null) {
            plugin.getLogger().warning("User not found in LuckPerms for " + player.getName());
            return 1.0;
        }

        ContextManager contextManager = plugin.getLuckPerms().getContextManager();
        ImmutableContextSet contextSet = contextManager.getContext(user).orElse(contextManager.getStaticContext());

        Set<String> multipliers = plugin.getConfig().getConfigurationSection("multipliers").getKeys(false);
        double totalMultiplier = 1.0;

        for (String multiplierKey : multipliers) {
            String permission = plugin.getConfig().getString("multipliers." + multiplierKey + ".permission");

            if (permission != null && user.getCachedData().getPermissionData(QueryOptions.contextual(contextSet)).checkPermission(permission).asBoolean()) {
                double currencyMultiplier = plugin.getConfig().getDouble("multipliers." + multiplierKey + "." + currency, 1.0);
                totalMultiplier += currencyMultiplier - 1.0;
            }
        }

        return totalMultiplier;
    }


    private double getPersonalMultiplier(Player player, String currency) {
        UUID playerUUID = player.getUniqueId();
        Map<String, Double> multipliers = plugin.getPlayerMultipliers(playerUUID);
        return multipliers.getOrDefault(currency, 1.0);
    }

    private long getTimeLeft(Player player, String currency) {
        UUID playerUUID = player.getUniqueId();
        Map<String, Long> expiryTimes = plugin.getPlayerMultiplierExpiry(playerUUID);
        long expiryTime = expiryTimes.getOrDefault(currency, 0L);
        return expiryTime - System.currentTimeMillis();
    }

    private String formatTimeLeft(long timeLeftMillis) {
        if (timeLeftMillis <= 0) {
            return "Expired";
        }

        long seconds = timeLeftMillis / 1000 % 60;
        long minutes = timeLeftMillis / 1000 / 60 % 60;
        long hours = timeLeftMillis / 1000 / 60 / 60 % 24;
        long days = timeLeftMillis / 1000 / 60 / 60 / 24;

        StringBuilder formattedTime = new StringBuilder();
        if (days > 0) formattedTime.append(days).append("d ");
        if (hours > 0 || days > 0) formattedTime.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) formattedTime.append(minutes).append("m ");
        formattedTime.append(seconds).append("s");

        return formattedTime.toString().trim();
    }
}
