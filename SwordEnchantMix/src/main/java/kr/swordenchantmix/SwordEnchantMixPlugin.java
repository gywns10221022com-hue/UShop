package kr.swordenchantmix;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class SwordEnchantMixPlugin extends JavaPlugin implements Listener {
    private static final Enchantment[] COMPATIBLE_ENCHANTMENTS = {
            Enchantment.SHARPNESS,
            Enchantment.SMITE,
            Enchantment.BANE_OF_ARTHROPODS
    };

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("날카로움, 강타, 살충의 모루 조합을 허용합니다.");
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack base = event.getInventory().getFirstItem();
        ItemStack addition = event.getInventory().getSecondItem();
        if (base == null || addition == null || !isSword(base) || !isSword(addition)) {
            return;
        }

        boolean hasTargetEnchantment = false;
        for (Enchantment enchantment : COMPATIBLE_ENCHANTMENTS) {
            if (base.containsEnchantment(enchantment) || addition.containsEnchantment(enchantment)) {
                hasTargetEnchantment = true;
                break;
            }
        }
        if (!hasTargetEnchantment) {
            return;
        }

        ItemStack result = base.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;

        for (Map.Entry<Enchantment, Integer> entry : addition.getEnchantments().entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            if (isTargetEnchantment(enchantment)) {
                int existing = meta.hasEnchant(enchantment) ? meta.getEnchantLevel(enchantment) : 0;
                meta.addEnchant(enchantment, Math.max(existing, level), true);
            }
        }

        result.setItemMeta(meta);
        event.setResult(result);
        event.getInventory().setRepairCost(5);
    }

    private boolean isSword(ItemStack item) {
        return switch (item.getType()) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD,
                    DIAMOND_SWORD, NETHERITE_SWORD -> true;
            default -> false;
        };
    }

    private boolean isTargetEnchantment(Enchantment enchantment) {
        return enchantment.equals(Enchantment.SHARPNESS)
                || enchantment.equals(Enchantment.SMITE)
                || enchantment.equals(Enchantment.BANE_OF_ARTHROPODS);
    }
}
