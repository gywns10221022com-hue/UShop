package kr.dragoneggbuff;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class DragonEggBuffPlugin extends JavaPlugin {

    private static final int EFFECT_DURATION_TICKS = 60;
    private static final int REFRESH_INTERVAL_TICKS = 20;

    @Override
    public void onEnable() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                if (!hasDragonEgg(player)) {
                    continue;
                }

                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.FIRE_RESISTANCE,
                    EFFECT_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true
                ));
                player.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE,
                    EFFECT_DURATION_TICKS,
                    0,
                    true,
                    false,
                    true
                ));
            }
        }, 0L, REFRESH_INTERVAL_TICKS);

        getLogger().info("DragonEggBuff enabled.");
    }

    private boolean hasDragonEgg(Player player) {
        return player.getInventory().contains(Material.DRAGON_EGG)
            || player.getInventory().getItemInOffHand().getType() == Material.DRAGON_EGG;
    }
}
