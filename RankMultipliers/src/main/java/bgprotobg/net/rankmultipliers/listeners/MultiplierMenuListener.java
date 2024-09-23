package bgprotobg.net.rankmultipliers.listeners;

import bgprotobg.net.rankmultipliers.RankMultipliers;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MultiplierMenuListener implements Listener {

    private final RankMultipliers plugin;

    public MultiplierMenuListener(RankMultipliers plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String configuredTitle = plugin.getConfig().getString("menu.inventory.title", "§aYour Multipliers").replace("&", "§");

        if (event.getView().getTitle().equals(configuredTitle)) {
            event.setCancelled(true);
        }
    }
}
