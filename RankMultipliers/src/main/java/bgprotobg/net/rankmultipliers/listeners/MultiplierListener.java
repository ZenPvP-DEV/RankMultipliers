package bgprotobg.net.rankmultipliers.listeners;

import bgprotobg.net.rankmultipliers.RankMultipliers;
import com.edwardbelt.edprison.events.EdPrisonAddMultiplierCurrency;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MultiplierListener implements Listener {

    private final RankMultipliers plugin;

    public MultiplierListener(RankMultipliers plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMultiplierAdd(EdPrisonAddMultiplierCurrency event) {
        FileConfiguration config = plugin.getConfig();
        UUID playerUUID = event.getUUID();
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return;
        }
        String playerName = player.getName();
        Map<String, Double> activeMultipliers = plugin.getPlayerMultipliers(playerUUID);
        if (activeMultipliers != null && activeMultipliers.containsKey(event.getCurrency().toLowerCase())) {
            double customMultiplier = activeMultipliers.get(event.getCurrency().toLowerCase());
            event.addMultiplier(customMultiplier);
        }

        User user = plugin.getLuckPerms().getUserManager().getUser(playerUUID);
        if (user == null) {
            plugin.getLogger().warning("User not found in LuckPerms for " + playerName);
            return;
        }

        ContextManager contextManager = plugin.getLuckPerms().getContextManager();
        ImmutableContextSet contextSet = contextManager.getContext(user).orElse(contextManager.getStaticContext());

        Set<String> multipliers = config.getConfigurationSection("multipliers").getKeys(false);
        for (String multiplierKey : multipliers) {
            String permission = config.getString("multipliers." + multiplierKey + ".permission");

            if (permission != null && user.getCachedData().getPermissionData(QueryOptions.contextual(contextSet)).checkPermission(permission).asBoolean()) {
                double tokensMultiplier = config.getDouble("multipliers." + multiplierKey + ".tokens", 1.0);
                double gemsMultiplier = config.getDouble("multipliers." + multiplierKey + ".gems", 1.0);
                double moneyMultiplier = config.getDouble("multipliers." + multiplierKey + ".money", 1.0);

                switch (event.getCurrency().toLowerCase()) {
                    case "tokens":
                        event.addMultiplier(tokensMultiplier);
                        break;
                    case "gems":
                        event.addMultiplier(gemsMultiplier);
                        break;
                    case "money":
                        event.addMultiplier(moneyMultiplier);
                        break;
                }
            }
        }
    }
}
