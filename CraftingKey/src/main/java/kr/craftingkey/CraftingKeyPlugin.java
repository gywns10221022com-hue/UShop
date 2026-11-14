package kr.craftingkey;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CraftingKeyPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onShiftSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        if (!player.isSneaking()) {
            return;
        }

        // Never intercept spear swaps: vanilla hand swapping is needed for spear-swap combos.
        if (isSpear(event.getMainHandItem().getType())
                || isSpear(event.getOffHandItem().getType())) {
            return;
        }

        event.setCancelled(true);

        // Open on the next tick so the hand-swap packet cannot override the GUI.
        getServer().getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                player.openWorkbench(null, true);
            }
        });
    }

    private boolean isSpear(Material material) {
        return material.name().endsWith("_SPEAR");
    }
}
