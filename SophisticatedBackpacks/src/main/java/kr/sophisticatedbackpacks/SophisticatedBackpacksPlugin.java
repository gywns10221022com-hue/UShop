package kr.sophisticatedbackpacks;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.util.*;

public final class SophisticatedBackpacksPlugin extends JavaPlugin implements Listener {
    private NamespacedKey backpackKey;
    private final Map<UUID, ItemStack> openedBackpacks = new HashMap<>();
    private final Map<UUID, Integer> openedSizes = new HashMap<>();

    private static final Material[] TIERS = {
        Material.LEATHER, Material.COPPER_INGOT, Material.IRON_INGOT,
        Material.GOLD_INGOT, Material.DIAMOND, Material.NETHERITE_INGOT
    };
    private static final String[] NAMES = {
        "가죽 백팩", "구리 백팩", "철 백팩", "금 백팩", "다이아몬드 백팩", "네더라이트 백팩"
    };
    private static final int[] SIZES = {18, 27, 36, 45, 54, 54};

    @Override
    public void onEnable() {
        backpackKey = new NamespacedKey(this, "backpack_tier");
        Bukkit.getPluginManager().registerEvents(this, this);
        registerRecipes();
        getLogger().info("SophisticatedBackpacks enabled.");
    }

    private ItemStack createBackpack(int tier) {
        ItemStack item = new ItemStack(Material.BUNDLE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6" + NAMES[tier]);
        meta.setLore(List.of("§7우클릭하여 보관함 열기", "§8저장 공간: " + SIZES[tier] + "칸"));
        meta.getPersistentDataContainer().set(backpackKey, PersistentDataType.INTEGER, tier);
        item.setItemMeta(meta);
        item.setAmount(1);
        return item;
    }

    private void registerRecipes() {
        for (int tier = 0; tier < TIERS.length; tier++) {
            NamespacedKey key = new NamespacedKey(this, "backpack_" + tier);
            ShapedRecipe recipe = new ShapedRecipe(key, createBackpack(tier));
            recipe.shape("MMM", "MBM", "MMM");
            recipe.setIngredient('M', TIERS[tier]);
            if (tier == 0) {
                recipe.setIngredient('B', Material.CHEST);
            } else {
                recipe.setIngredient('B', new RecipeChoice.ExactChoice(createBackpack(tier - 1)));
            }
            Bukkit.addRecipe(recipe);
        }
    }

    private Integer getTier(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(backpackKey, PersistentDataType.INTEGER);
    }

    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        Integer tier = getTier(item);
        if (tier == null) return;
        event.setCancelled(true);
        openBackpack(event.getPlayer(), item, tier);
    }

    private void openBackpack(Player player, ItemStack backpack, int tier) {
        int size = SIZES[Math.max(0, Math.min(tier, SIZES.length - 1))];
        Inventory inventory = Bukkit.createInventory(null, size, "§8" + NAMES[tier]);
        ItemMeta meta = backpack.getItemMeta();
        byte[] stored = meta.getPersistentDataContainer().get(
            new NamespacedKey(this, "contents"), PersistentDataType.BYTE_ARRAY);
        if (stored != null) {
            try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(stored))) {
                Object obj = in.readObject();
                if (obj instanceof ItemStack[] contents) inventory.setContents(contents);
            } catch (Exception ex) {
                getLogger().warning("백팩 데이터 읽기 실패: " + ex.getMessage());
            }
        }
        player.openInventory(inventory);
        openedBackpacks.put(player.getUniqueId(), backpack);
        openedSizes.put(player.getUniqueId(), size);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        ItemStack backpack = openedBackpacks.remove(id);
        openedSizes.remove(id);
        if (backpack == null || !backpack.hasItemMeta()) return;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(bytes)) {
                out.writeObject(event.getInventory().getContents());
            }
            ItemMeta meta = backpack.getItemMeta();
            meta.getPersistentDataContainer().set(
                new NamespacedKey(this, "contents"), PersistentDataType.BYTE_ARRAY, bytes.toByteArray());
            backpack.setItemMeta(meta);
        } catch (IOException ex) {
            getLogger().severe("백팩 데이터 저장 실패: " + ex.getMessage());
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (openedBackpacks.containsKey(player.getUniqueId()) && event.getClickedInventory() == player.getInventory()) {
            // 일반 인벤토리 이동은 허용
        }
    }
}
