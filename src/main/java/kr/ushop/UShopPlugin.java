package kr.ushop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.util.*;

public final class UShopPlugin extends JavaPlugin implements Listener {
    private YamlConfiguration data;
    private File dataFile;
    private NamespacedKey listingKey;
    private static final String TITLE = "§8유저상점";
    private final Map<UUID, Integer> pages = new HashMap<>();

    @Override public void onEnable() {
        listingKey = new NamespacedKey(this, "listing_id");
        dataFile = new File(getDataFolder(), "shops.yml");
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        data = YamlConfiguration.loadConfiguration(dataFile);
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("UShop enabled.");
    }
    @Override public void onDisable() { saveData(); }
    private void saveData() { try { data.save(dataFile); } catch (IOException e) { getLogger().severe("Could not save shops.yml: " + e.getMessage()); } }
    private String path(String id) { return "listings." + id; }
    private int diamonds(Player p) { return p.getInventory().all(Material.DIAMOND).values().stream().mapToInt(ItemStack::getAmount).sum(); }
    private boolean takeDiamonds(Player p, int amount) {
        if (diamonds(p) < amount) return false;
        int left = amount;
        for (int slot = 0; slot < p.getInventory().getSize() && left > 0; slot++) {
            ItemStack it = p.getInventory().getItem(slot);
            if (it == null || it.getType() != Material.DIAMOND) continue;
            int n = Math.min(left, it.getAmount()); left -= n;
            if (it.getAmount() == n) p.getInventory().setItem(slot, null); else it.setAmount(it.getAmount() - n);
        }
        return true;
    }
    private void give(Player p, ItemStack item) {
        Map<Integer, ItemStack> overflow = p.getInventory().addItem(item);
        overflow.values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
    }
    private String encode(ItemStack item) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream out = new BukkitObjectOutputStream(b)) { out.writeObject(item); }
        return Base64.getEncoder().encodeToString(b.toByteArray());
    }
    private ItemStack decode(String s) throws IOException, ClassNotFoundException {
        byte[] b = Base64.getDecoder().decode(s);
        try (BukkitObjectInputStream in = new BukkitObjectInputStream(new ByteArrayInputStream(b))) { return (ItemStack) in.readObject(); }
    }
    private void openShop(Player p, int page) {
        pages.put(p.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE + " §7(" + (page+1) + ")");
        List<String> ids = new ArrayList<>();
        ConfigurationSection sec = data.getConfigurationSection("listings");
        if (sec != null) ids.addAll(sec.getKeys(false));
        ids.sort(Comparator.comparingLong(id -> data.getLong(path(id)+".created")));
        int start = page * 45;
        for (int i = start; i < Math.min(start + 45, ids.size()); i++) {
            String id = ids.get(i); String base = path(id);
            try {
                ItemStack item = decode(data.getString(base+".item", ""));
                ItemMeta meta = item.getItemMeta();
                List<String> lore = meta != null && meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("§7판매자: §f"+data.getString(base+".sellerName"));
                lore.add("§b가격: §f"+data.getInt(base+".price")+" 다이아");
                lore.add("§e클릭하여 구매");
                if (meta != null) { meta.setLore(lore); meta.getPersistentDataContainer().set(listingKey, PersistentDataType.STRING, id); item.setItemMeta(meta); }
                inv.setItem(i-start, item);
            } catch (Exception ex) { getLogger().warning("Invalid listing " + id); }
        }
        inv.setItem(45, button(Material.ARROW, "§e이전 페이지"));
        inv.setItem(49, button(Material.BARRIER, "§c닫기"));
        inv.setItem(53, button(Material.ARROW, "§e다음 페이지"));
        p.openInventory(inv);
    }
    private ItemStack button(Material m, String name) { ItemStack i = new ItemStack(m); ItemMeta meta=i.getItemMeta(); meta.setDisplayName(name); i.setItemMeta(meta); return i; }
    @EventHandler public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || e.getView().getTitle().startsWith(TITLE) == false) return;
        e.setCancelled(true);
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        int slot=e.getRawSlot();
        if (slot==49) { p.closeInventory(); return; }
        if (slot==45) { openShop(p, Math.max(0,pages.getOrDefault(p.getUniqueId(),0)-1)); return; }
        if (slot==53) { openShop(p,pages.getOrDefault(p.getUniqueId(),0)+1); return; }
        ItemStack clicked=e.getCurrentItem();
        if (clicked==null || !clicked.hasItemMeta()) return;
        String id=clicked.getItemMeta().getPersistentDataContainer().get(listingKey,PersistentDataType.STRING);
        if (id==null || !data.contains(path(id))) return;
        String base=path(id); UUID seller;
        try { seller=UUID.fromString(data.getString(base+".seller")); } catch(Exception ex) { return; }
        if (seller.equals(p.getUniqueId())) { p.sendMessage("§c자신의 상품은 구매할 수 없습니다."); return; }
        int price=data.getInt(base+".price");
        if (diamonds(p)<price) { p.sendMessage("§c다이아가 부족합니다. 필요: "+price); return; }
        try {
            ItemStack item=decode(data.getString(base+".item"));
            if (!takeDiamonds(p,price)) return;
            give(p,item);
            String earn="earnings."+seller;
            data.set(earn,data.getInt(earn)+price);
            data.set(base,null); saveData();
            p.sendMessage("§a구매 완료! §f"+price+" 다이아를 지불했습니다.");
            openShop(p,pages.getOrDefault(p.getUniqueId(),0));
        } catch(Exception ex) { p.sendMessage("§c구매 처리 중 오류가 발생했습니다."); getLogger().severe("Purchase failed: "+ex.getMessage()); }
    }
    @Override public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("플레이어만 사용할 수 있습니다."); return true; }
        switch(cmd.getName().toLowerCase(Locale.ROOT)) {
            case "ushop" -> {
                if (args.length==0) openShop(p,0);
                else {
                    int price; try { price=Integer.parseInt(args[0]); } catch(NumberFormatException ex) { p.sendMessage("§c사용법: /유저상점 <다이아가격>"); return true; }
                    ItemStack hand=p.getInventory().getItemInMainHand();
                    if (price<1 || hand.getType()==Material.AIR) { p.sendMessage("§c손에 아이템을 들고 1 이상의 가격을 입력하세요."); return true; }
                    String id=UUID.randomUUID().toString();
                    try { data.set(path(id)+".seller",p.getUniqueId().toString()); data.set(path(id)+".sellerName",p.getName()); data.set(path(id)+".price",price); data.set(path(id)+".item",encode(hand.clone())); data.set(path(id)+".created",System.currentTimeMillis());
                        p.getInventory().setItemInMainHand(null); saveData(); p.sendMessage("§a상점에 등록했습니다! ID: "+id+" / 가격: "+price+" 다이아");
                    } catch(Exception ex) { p.sendMessage("§c등록 실패: 아이템 저장 오류"); data.set(path(id),null); }
                }
            }
            case "myshop" -> {
                ConfigurationSection sec=data.getConfigurationSection("listings"); int count=0;
                if(sec!=null) for(String id:sec.getKeys(false)) if(p.getUniqueId().toString().equals(data.getString(path(id)+".seller"))) { p.sendMessage("§eID: "+id+" §f가격: "+data.getInt(path(id)+".price")+" 다이아"); count++; }
                p.sendMessage("§a등록 상품: "+count+"개. 취소: /판매취소 <ID>");
            }
            case "shopcancel" -> {
                if(args.length!=1 || !data.contains(path(args[0]))) { p.sendMessage("§c사용법: /판매취소 <ID>"); return true; }
                String b=path(args[0]); if(!p.getUniqueId().toString().equals(data.getString(b+".seller"))) { p.sendMessage("§c본인 상품만 취소할 수 있습니다."); return true; }
                try { give(p,decode(data.getString(b+".item"))); data.set(b,null); saveData(); p.sendMessage("§a판매를 취소하고 아이템을 돌려받았습니다."); } catch(Exception ex) { p.sendMessage("§c아이템 복구 실패"); }
            }
            case "shopclaim" -> {
                String earn="earnings."+p.getUniqueId(); int amount=data.getInt(earn);
                if(amount<1) { p.sendMessage("§7수령할 판매금이 없습니다."); return true; }
                int left=amount; while(left>0) { int n=Math.min(64,left); give(p,new ItemStack(Material.DIAMOND,n)); left-=n; }
                data.set(earn,0); saveData(); p.sendMessage("§a판매금 "+amount+" 다이아를 수령했습니다.");
            }
        }
        return true;
    }
}
