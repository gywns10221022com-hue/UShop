package kr.autosmeltores;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class AutoSmeltOresPlugin extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("철광석과 금광석 자동 제련 기능이 활성화되었습니다.");
    }

    @EventHandler
    public void onBlockDrop(BlockDropItemEvent event) {
        // Silk Touch drops the ore block itself, so it is intentionally left unchanged.
        for (Item dropped : event.getItems()) {
            ItemStack stack = dropped.getItemStack();
            Material smelted = getSmeltedMaterial(stack.getType());
            if (smelted == null) continue;

            ItemStack result = new ItemStack(smelted, stack.getAmount());
            result.setItemMeta(stack.getItemMeta());
            dropped.setItemStack(result);
        }
    }

    private Material getSmeltedMaterial(Material material) {
        return switch (material) {
            case RAW_IRON -> Material.IRON_INGOT;
            case RAW_GOLD -> Material.GOLD_INGOT;
            default -> null;
        };
    }
}
