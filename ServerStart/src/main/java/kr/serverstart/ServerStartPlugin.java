package kr.serverstart;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerStartPlugin extends JavaPlugin implements Listener {
    private boolean started;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        started = getConfig().getBoolean("started", false);
        Bukkit.getPluginManager().registerEvents(this, this);

        for (World world : Bukkit.getWorlds()) {
            applyWorldSettings(world);
        }

        if (!started) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage("§e서버가 아직 시작되지 않았습니다. 관리자의 시작을 기다려 주세요.");
            }
        }
        getLogger().info(started ? "서버 시작 상태로 로드했습니다." : "대기 상태로 로드했습니다. OP가 /시작을 입력해야 합니다.");
    }

    private void applyWorldSettings(World world) {
        world.getWorldBorder().setCenter(0.0, 0.0);
        world.getWorldBorder().setSize(100000.0);
        world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 0);

        // locatorBar gamerule is available on modern Minecraft versions.
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute in " + world.getKey()
                + " run gamerule locatorBar false");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!started) {
            event.getPlayer().sendMessage("§e서버가 아직 시작되지 않았습니다. 관리자의 시작을 기다려 주세요.");
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (started || event.getTo() == null) return;
        if (event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getY() != event.getTo().getY()
                || event.getFrom().getZ() != event.getTo().getZ()) {
            event.setTo(event.getFrom());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("시작")) return false;
        if (!sender.isOp() && !sender.hasPermission("serverstart.start")) {
            sender.sendMessage("§c이 명령어는 OP만 사용할 수 있습니다.");
            return true;
        }
        if (started) {
            sender.sendMessage("§e서버는 이미 시작되었습니다.");
            return true;
        }

        started = true;
        getConfig().set("started", true);
        saveConfig();

        for (World world : Bukkit.getWorlds()) {
            applyWorldSettings(world);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 32))
                    .values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        }

        Bukkit.broadcastMessage("§a§l서버가 시작되었습니다!");
        Bukkit.broadcastMessage("§e모든 플레이어에게 구운 소고기 32개가 지급되었습니다.");
        sender.sendMessage("§a서버 시작 처리가 완료되었습니다.");
        return true;
    }
}
