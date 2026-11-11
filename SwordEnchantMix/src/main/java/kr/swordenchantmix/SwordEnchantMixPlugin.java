package kr.swordenchantmix;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

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

    // Run after ordinary anvil handlers so EnchantLimit's result and cost are preserved.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack base = event.getInventory().getFirstItem();
        ItemStack addition = event.getInventory().getSecondItem();
        if (base == null || addition == null || !isSword(base) || !isSword(addition)) return;

        boolean hasTargetEnchantment = false;
        for (Enchantment enchantment : COMPATIBLE_ENCHANTMENTS) {
            if (base.containsEnchantment(enchantment) || addition.containsEnchantment(enchantment)) {
                hasTargetEnchantment = true;
                break;
            }
        }
        if (!hasTargetEnchantment) return;

        // Keep another plugin's prepared result when one exists; otherwise allow
        // the otherwise-invalid Sharpness/Smite/Bane combination from the base sword.
        ItemStack result = event.getResult() == null ? base.clone() : event.getResult().clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;

        for (Enchantment enchantment : COMPATIBLE_ENCHANTMENTS) {
            int level = Math.max(base.getEnchantmentLevel(enchantment),
                    addition.getEnchantmentLevel(enchantment));
            if (meta.hasEnchant(enchantment)) {
                level = Math.max(level, meta.getEnchantLevel(enchantment));
            }
            if (level > 0) {
                // Ignore vanilla incompatibility checks, but leave level caps to EnchantLimit.
                meta.addEnchant(enchantment, level, true);
            }
        }

        result.setItemMeta(meta);
        event.setResult(result);

        // Do not overwrite EnchantLimit's calculated anvil cost.
        if (event.getInventory().getRepairCost() < 1) {
            event.getInventory().setRepairCost(5);
        }
    }

    private boolean isSword(ItemStack item) {
        return switch (item.getType()) {
            case WOODEN_SWORD, STONE_SWORD, IRON_SWORD, GOLDEN_SWORD,
                    DIAMOND_SWORD, NETHERITE_SWORD -> true;
            default -> false;
        };
    }
}
