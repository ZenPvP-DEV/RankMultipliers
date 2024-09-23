package bgprotobg.net.rankmultipliers.listeners;

import bgprotobg.net.rankmultipliers.RankMultipliers;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final RankMultipliers plugin;

    public PlayerJoinListener(RankMultipliers plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.loadPlayerMultipliers(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }
}
