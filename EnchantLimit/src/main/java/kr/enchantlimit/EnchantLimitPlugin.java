package kr.enchantlimit;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class EnchantLimitPlugin extends JavaPlugin implements Listener {
    private static final int MAX_SHARPNESS = 6;
    private static final int MAX_UNBREAKING = 10;
    private static final int MAX_EFFICIENCY = 7;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("EnchantLimit enabled: Sharpness 6 (+1 damage per level), Unbreaking 10, Efficiency 7; Protection uses vanilla behavior.");
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        // Remove the vanilla "Too Expensive" repair-cost ceiling.
        inventory.setMaximumRepairCost(Integer.MAX_VALUE);

        ItemStack left = inventory.getItem(0);
        ItemStack right = inventory.getItem(1);
        if (left == null || right == null
                || left.getType() == Material.AIR || right.getType() == Material.AIR) return;

        ItemStack vanillaResult = event.getResult();
        if (vanillaResult == null) return;

        ItemStack output = vanillaResult.clone();
        ItemMeta outputMeta = output.getItemMeta();
        ItemMeta leftMeta = left.getItemMeta();
        if (outputMeta == null || leftMeta == null) return;

        Map<Enchantment, Integer> incoming = getEnchantments(right);
        int totalCost = 0;
        boolean changed = false;

        for (Map.Entry<Enchantment, Integer> entry : incoming.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int cap = getCap(enchantment);
            if (cap == 0) continue;

            int current = getLevel(leftMeta, enchantment);
            int offered = entry.getValue();
            int combined = current == offered ? current + 1 : Math.max(current, offered);
            int level = Math.min(cap, combined);
            if (level <= 0) continue;

            if (outputMeta instanceof EnchantmentStorageMeta bookMeta) {
                bookMeta.addStoredEnchant(enchantment, level, true);
            } else {
                outputMeta.addEnchant(enchantment, level, true);
            }

            totalCost += level * 2 + Math.max(0, level - 5) * 3;
            changed = true;
        }

        if (changed) {
            output.setItemMeta(outputMeta);
            event.setResult(output);
            inventory.setRepairCost(Math.max(1, totalCost));
        }
    }

    // Each Sharpness level adds exactly 1 bonus damage.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSharpnessDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof LivingEntity attacker)) return;
        ItemStack weapon = attacker.getEquipment() == null ? null : attacker.getEquipment().getItemInMainHand();
        if (weapon == null || weapon.getType().isAir()) return;

        int sharpness = weapon.getEnchantmentLevel(Enchantment.SHARPNESS);
        if (sharpness > 0) {
            event.setDamage(event.getDamage() + sharpness);
        }
    }

    private int getLevel(ItemMeta meta, Enchantment enchantment) {
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            return bookMeta.getStoredEnchantLevel(enchantment);
        }
        return meta.getEnchantLevel(enchantment);
    }

    private Map<Enchantment, Integer> getEnchantments(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta bookMeta) return bookMeta.getStoredEnchants();
        return meta == null ? Map.of() : meta.getEnchants();
    }

    private int getCap(Enchantment enchantment) {
        if (enchantment.equals(Enchantment.SHARPNESS)) return MAX_SHARPNESS;
        if (enchantment.equals(Enchantment.UNBREAKING)) return MAX_UNBREAKING;
        if (enchantment.equals(Enchantment.EFFICIENCY)) return MAX_EFFICIENCY;
        return 0;
    }
}