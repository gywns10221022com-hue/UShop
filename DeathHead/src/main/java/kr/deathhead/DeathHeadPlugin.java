package kr.deathhead;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class DeathHeadPlugin extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("DeathHead 활성화: 플레이어 사망 시 머리를 드롭합니다.");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(event.getEntity());
            meta.setDisplayName(event.getEntity().getName() + "의 머리");
            head.setItemMeta(meta);
        }
        event.getDrops().add(head);
    }
}
