package kr.playertracker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerTrackerPlugin extends JavaPlugin implements Listener {
    private NamespacedKey compassKey;
    private final Map<UUID, UUID> targets = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> displayEnabled = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        compassKey = new NamespacedKey(this, "tracker_compass");
        getServer().getPluginManager().registerEvents(this, this);

        if (getCommand("trackercompass") != null) {
            getCommand("추적나침반").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("플레이어만 사용할 수 있습니다.");
                    return true;
                }
                player.getInventory().addItem(createCompass()).values()
                        .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                player.sendMessage(ChatColor.GREEN + "추적 나침반을 받았습니다. 인벤토리에 보유하면 추적이 활성화됩니다.");
                displayEnabled.putIfAbsent(player.getUniqueId(), true);
                return true;
            });
        }

        Bukkit.getScheduler().runTaskTimer(this, this::updateActionBars, 10L, 10L);
        getLogger().info("PlayerTracker enabled.");
    }

    private ItemStack createCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "추적 나침반");
        meta.setLore(List.of(
                ChatColor.GRAY + "인벤토리에 보유하면 추적 정보가 표시됩니다.",
                ChatColor.YELLOW + "우클릭: 추적 표시 켜기/끄기",
                ChatColor.YELLOW + "쉬프트 + 우클릭: 다음 플레이어 선택"
        ));
        meta.getPersistentDataContainer().set(compassKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isTrackerCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) return false;
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(compassKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    private boolean hasTrackerCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isTrackerCompass(item)) return true;
        }
        return false;
    }

    @EventHandler
    public void onCompassUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!isTrackerCompass(event.getItem())) return;

        event.setCancelled(true);
        if (!hasTrackerCompass(player)) return;

        if (player.isSneaking()) {
            selectNextTarget(player);
        } else {
            boolean enabled = !displayEnabled.getOrDefault(player.getUniqueId(), true);
            displayEnabled.put(player.getUniqueId(), enabled);
            player.sendMessage(enabled
                    ? ChatColor.GREEN + "추적 표시를 켰습니다."
                    : ChatColor.YELLOW + "추적 표시를 껐습니다.");
        }
    }

    private void selectNextTarget(Player player) {
        List<Player> candidates = new ArrayList<>(Bukkit.getOnlinePlayers());
        candidates.removeIf(candidate -> candidate.getUniqueId().equals(player.getUniqueId()));
        candidates.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        if (candidates.isEmpty()) {
            targets.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "선택할 다른 플레이어가 없습니다.");
            return;
        }

        UUID current = targets.get(player.getUniqueId());
        int currentIndex = -1;
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).getUniqueId().equals(current)) {
                currentIndex = i;
                break;
            }
        }
        Player next = candidates.get((currentIndex + 1) % candidates.size());
        targets.put(player.getUniqueId(), next.getUniqueId());
        player.sendMessage(ChatColor.AQUA + "추적 대상: " + ChatColor.WHITE + next.getName());
    }

    private void updateActionBars() {
        for (Player tracker : Bukkit.getOnlinePlayers()) {
            if (!hasTrackerCompass(tracker)) continue;
            if (!displayEnabled.getOrDefault(tracker.getUniqueId(), true)) continue;

            Player target = findTarget(tracker);
            if (target == null) {
                tracker.sendActionBar(ChatColor.GRAY + "추적 대상이 없습니다. 나침반 쉬프트+우클릭으로 선택하세요.");
                continue;
            }

            if (!tracker.getWorld().getUID().equals(target.getWorld().getUID())) {
                tracker.sendActionBar(ChatColor.RED + target.getName() + ChatColor.GRAY + " 님은 다른 차원에 있습니다.");
                continue;
            }

            double distance = tracker.getLocation().distance(target.getLocation());
            tracker.sendActionBar(ChatColor.AQUA + "추적 대상: " + ChatColor.WHITE + target.getName()
                    + ChatColor.DARK_GRAY + " | " + ChatColor.YELLOW + String.format("%.1f", distance) + "m");
        }
    }

    private Player findTarget(Player tracker) {
        UUID targetId = targets.get(tracker.getUniqueId());
        if (targetId != null) {
            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) return target;
        }

        List<Player> candidates = new ArrayList<>(Bukkit.getOnlinePlayers());
        candidates.removeIf(candidate -> candidate.getUniqueId().equals(tracker.getUniqueId()));
        candidates.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        if (candidates.isEmpty()) return null;

        Player first = candidates.get(0);
        targets.put(tracker.getUniqueId(), first.getUniqueId());
        return first;
    }

    @Override
    public void onDisable() {
        targets.clear();
        displayEnabled.clear();
    }
}
