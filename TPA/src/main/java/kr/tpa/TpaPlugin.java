package kr.tpa;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TpaPlugin extends JavaPlugin implements CommandExecutor {
    private static final long REQUEST_TIMEOUT_TICKS = 60L * 20L;
    private final Map<UUID, UUID> incoming = new HashMap<>();
    private final Map<UUID, UUID> outgoing = new HashMap<>();

    @Override
    public void onEnable() {
        getCommand("tpa").setExecutor(this);
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
                UUID id = event.getPlayer().getUniqueId();
                UUID requester = incoming.remove(id);
                if (requester != null) {
                    outgoing.remove(requester);
                    Player p = Bukkit.getPlayer(requester);
                    if (p != null) p.sendMessage(color("&c상대방이 서버를 나가 요청이 취소되었습니다."));
                }
                UUID target = outgoing.remove(id);
                if (target != null) {
                    incoming.remove(target);
                    Player p = Bukkit.getPlayer(target);
                    if (p != null) p.sendMessage(color("&c요청자가 서버를 나가 요청이 취소되었습니다."));
                }
            }
        }, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있는 명령어입니다.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(color("&e사용법: /tpa <플레이어 이름>"));
            player.sendMessage(color("&e요청 수락: /tpa 찬성  |  거절: /tpa 거절"));
            return true;
        }

        String action = args[0];
        if (action.equalsIgnoreCase("찬성") || action.equalsIgnoreCase("accept")) {
            accept(player);
            return true;
        }
        if (action.equalsIgnoreCase("거절") || action.equalsIgnoreCase("deny")) {
            deny(player);
            return true;
        }

        Player target = Bukkit.getPlayerExact(action);
        if (target == null) {
            player.sendMessage(color("&c해당 플레이어가 접속 중이지 않습니다."));
            return true;
        }
        if (target.equals(player)) {
            player.sendMessage(color("&c자기 자신에게는 요청할 수 없습니다."));
            return true;
        }
        if (outgoing.containsKey(player.getUniqueId())) {
            player.sendMessage(color("&c이미 보낸 TPA 요청이 있습니다."));
            return true;
        }
        if (incoming.containsKey(target.getUniqueId())) {
            player.sendMessage(color("&c상대방에게 처리되지 않은 요청이 있습니다."));
            return true;
        }

        UUID requesterId = player.getUniqueId();
        UUID targetId = target.getUniqueId();
        incoming.put(targetId, requesterId);
        outgoing.put(requesterId, targetId);
        player.sendMessage(color("&a" + target.getName() + "님에게 순간이동 요청을 보냈습니다."));
        target.sendMessage(color("&e" + player.getName() + "님이 당신에게 순간이동을 요청했습니다."));
        target.sendMessage(color("&a수락: &f/tpa 찬성   &c거절: &f/tpa 거절"));
        target.sendMessage(color("&7요청은 60초 후 자동으로 만료됩니다."));

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (requesterId.equals(incoming.get(targetId))) {
                incoming.remove(targetId);
                outgoing.remove(requesterId);
                Player requester = Bukkit.getPlayer(requesterId);
                Player receiver = Bukkit.getPlayer(targetId);
                if (requester != null) requester.sendMessage(color("&cTPA 요청 시간이 만료되었습니다."));
                if (receiver != null) receiver.sendMessage(color("&cTPA 요청이 만료되었습니다."));
            }
        }, REQUEST_TIMEOUT_TICKS);
        return true;
    }

    private void accept(Player target) {
        UUID targetId = target.getUniqueId();
        UUID requesterId = incoming.remove(targetId);
        if (requesterId == null) {
            target.sendMessage(color("&c받은 TPA 요청이 없습니다."));
            return;
        }
        outgoing.remove(requesterId);
        Player requester = Bukkit.getPlayer(requesterId);
        if (requester == null) {
            target.sendMessage(color("&c요청자가 접속 중이지 않습니다."));
            return;
        }
        requester.teleport(target.getLocation());
        requester.sendMessage(color("&a" + target.getName() + "님에게 이동했습니다."));
        target.sendMessage(color("&a" + requester.getName() + "님의 TPA 요청을 수락했습니다."));
    }

    private void deny(Player target) {
        UUID targetId = target.getUniqueId();
        UUID requesterId = incoming.remove(targetId);
        if (requesterId == null) {
            target.sendMessage(color("&c받은 TPA 요청이 없습니다."));
            return;
        }
        outgoing.remove(requesterId);
        Player requester = Bukkit.getPlayer(requesterId);
        target.sendMessage(color("&c" + (requester == null ? "플레이어" : requester.getName()) + "님의 요청을 거절했습니다."));
        if (requester != null) requester.sendMessage(color("&c" + target.getName() + "님이 TPA 요청을 거절했습니다."));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
