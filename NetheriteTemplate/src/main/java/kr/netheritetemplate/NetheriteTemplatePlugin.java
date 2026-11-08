package kr.netheritetemplate;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class NetheriteTemplatePlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        NamespacedKey key = new NamespacedKey(this, "netherite_upgrade_template");
        ShapedRecipe recipe = new ShapedRecipe(
                key,
                new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
        );

        recipe.shape("ANA", "AIA", "AAA");
        recipe.setIngredient('A', Material.ANCIENT_DEBRIS);
        recipe.setIngredient('N', Material.NETHERRACK);
        recipe.setIngredient('I', Material.NETHERITE_INGOT);

        Bukkit.removeRecipe(key);
        if (Bukkit.addRecipe(recipe)) {
            getLogger().info("네더라이트 강화 형판 제작법이 등록되었습니다.");
        } else {
            getLogger().warning("네더라이트 강화 형판 제작법 등록에 실패했습니다.");
        }
    }
}
