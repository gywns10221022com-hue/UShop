package kr.villagertradefix;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTradeEvent;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class VillagerTradeFixPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("VillagerTradeFix enabled. Villager trades will not lock.");
    }

    @EventHandler
    public void onPlayerTrade(PlayerTradeEvent event) {
        AbstractVillager villager = event.getVillager();
        MerchantRecipe tradedRecipe = event.getTrade();

        // Run one tick later so the trade's use count has already been updated.
        getServer().getScheduler().runTask(this, () -> {
            List<MerchantRecipe> recipes = new ArrayList<>(villager.getRecipes());
            boolean changed = false;

            for (MerchantRecipe recipe : recipes) {
                if (recipe == tradedRecipe || recipe.equals(tradedRecipe)) {
                    recipe.setUses(0);
                    changed = true;
                    break;
                }
            }

            if (changed && villager.isValid()) {
                villager.setRecipes(recipes);
            }
        });
    }
}
