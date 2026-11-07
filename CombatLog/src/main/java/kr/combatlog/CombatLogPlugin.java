package kr.combatlog;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatLogPlugin extends JavaPlugin implements Listener {
    private static final long COMBAT_TIME_MILLIS = 15_000L;
    private final Map<UUID, Long> combatUntil = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("CombatLog enabled. Combat tag: 15 seconds.");
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) return;

        tag(attacker);
        tag(victim);
        attacker.sendActionBar(ChatColor.RED + "⚔ 전투 중! 15초 동안 로그아웃하면 사망합니다.");
        victim.sendActionBar(ChatColor.RED + "⚔ 전투 중! 15초 동안 로그아웃하면 사망합니다.");
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }

    private void tag(Player player) {
        combatUntil.put(player.getUniqueId(), System.currentTimeMillis() + COMBAT_TIME_MILLIS);
    }

    private boolean isInCombat(Player player) {
        Long until = combatUntil.get(player.getUniqueId());
        if (until == null) return false;
        if (until <= System.currentTimeMillis()) {
            combatUntil.remove(player.getUniqueId(), until);
            return false;
        }
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!isInCombat(player)) {
            combatUntil.remove(player.getUniqueId());
            return;
        }

        combatUntil.remove(player.getUniqueId());
        // A zero-health transition fires the normal Bukkit death flow before disconnect completes.
        if (player.isOnline() && !player.isDead()) {
            player.setHealth(0.0);
            Bukkit.broadcastMessage(ChatColor.RED + player.getName()
                    + "님이 전투 중 로그아웃하여 사망했습니다.");
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        combatUntil.remove(event.getEntity().getUniqueId());
    }

    @Override
    public void onDisable() {
        combatUntil.clear();
    }
}
