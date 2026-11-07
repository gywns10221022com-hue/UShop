package kr.villagertradefix;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class VillagerTradeFixPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("VillagerTradeFix enabled. Villager trades will not lock.");
    }

    @EventHandler
    public void onPlayerTrade(PlayerTradeEvent event) {
        // Do not increase the trade's usage count, so it never reaches the red-X limit.
        event.setIncreaseTradeUses(false);
    }
}
