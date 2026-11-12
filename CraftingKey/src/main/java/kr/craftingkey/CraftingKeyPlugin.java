package kr.craftingkey;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CraftingKeyPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onShiftSwapHands(PlayerSwapHandItemsEvent event) {
        if (!event.getPlayer().isSneaking()) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().openWorkbench(null, true);
    }
}
