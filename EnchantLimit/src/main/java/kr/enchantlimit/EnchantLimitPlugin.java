package kr.enchantlimit;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class EnchantLimitPlugin extends JavaPlugin implements Listener {
    private static final int MAX_SHARPNESS = 10;
    private static final int MAX_PROTECTION = 7;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("EnchantLimit enabled: Sharpness 10, Protection 7; unlimited anvil cost.");
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        inventory.setMaximumRepairCost(Integer.MAX_VALUE);

        ItemStack left = inventory.getItem(0);
        ItemStack right = inventory.getItem(1);
        if (left == null || right == null
                || left.getType() == Material.AIR || right.getType() == Material.AIR) return;

        ItemStack output = event.getResult() == null ? left.clone() : event.getResult().clone();
        ItemMeta outputMeta = output.getItemMeta();
        if (outputMeta == null) return;

        Map<Enchantment, Integer> incoming = getEnchantments(right);
        boolean changed = false;

        for (Map.Entry<Enchantment, Integer> entry : incoming.entrySet()) {
            Enchantment enchantment = entry.getKey();
            int cap = getCap(enchantment);
            if (cap == 0) continue;

            int current = getLevel(outputMeta, enchantment);
            int offered = entry.getValue();
            int combined = current == offered ? current + 1 : Math.max(current, offered);
            int level = Math.min(cap, combined);

            if (level <= current) continue;

            // Enchanted books store enchantments separately from normal item metadata.
            if (outputMeta instanceof EnchantmentStorageMeta bookMeta) {
                bookMeta.addStoredEnchant(enchantment, level, true);
            } else {
                outputMeta.addEnchant(enchantment, level, true);
            }
            changed = true;
        }

        if (changed) {
            output.setItemMeta(outputMeta);
            event.setResult(output);
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
        if (enchantment.equals(Enchantment.PROTECTION)) return MAX_PROTECTION;
        return 0;
    }
}
