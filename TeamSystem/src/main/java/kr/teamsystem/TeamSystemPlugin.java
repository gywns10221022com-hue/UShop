package kr.teamsystem;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.Material;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class TeamSystemPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private static final Map<String, ChatColor> COLORS = new LinkedHashMap<>();
    static {
        COLORS.put("빨강", ChatColor.RED); COLORS.put("주황", ChatColor.GOLD);
        COLORS.put("노랑", ChatColor.YELLOW); COLORS.put("초록", ChatColor.GREEN);
        COLORS.put("청록", ChatColor.DARK_AQUA); COLORS.put("파랑", ChatColor.BLUE);
        COLORS.put("보라", ChatColor.LIGHT_PURPLE); COLORS.put("분홍", ChatColor.LIGHT_PURPLE);
        COLORS.put("흰색", ChatColor.WHITE); COLORS.put("회색", ChatColor.GRAY);
        COLORS.put("검정", ChatColor.BLACK); COLORS.put("금색", ChatColor.GOLD);
    }
    private final Map<String, TeamData> teams = new HashMap<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Map<UUID, String> teamInvites = new HashMap<>();
    private final Map<String, String> allianceInvites = new HashMap<>();
    private final Set<UUID> betrayers = new HashSet<>();
    private static final String COLOR_GUI="팀 이름표 색상 선택", LIST_GUI="전체 팀 목록";

    @Override public void onEnable() {
        loadData();
        for (String cmd : List.of("팀생성", "팀", "배신", "동맹", "팀장")) getCommand(cmd).setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        refreshOnlineNames();
        getLogger().info("TeamSystem enabled");
    }
    @Override public void onDisable() { saveData(); }

    private static class TeamData {
        String name; UUID leader; ChatColor color = ChatColor.WHITE;
        Set<UUID> members = new HashSet<>(); Set<String> allies = new HashSet<>();
        TeamData(String name, UUID leader) { this.name=name; this.leader=leader; members.add(leader); }
    }
    private String key(String name) { return "ts" + Integer.toHexString(name.toLowerCase(Locale.ROOT).hashCode()); }
    private TeamData teamOf(UUID id) { String n=playerTeams.get(id); return n==null?null:teams.get(n.toLowerCase(Locale.ROOT)); }
    private void msg(CommandSender s, String m) { s.sendMessage(ChatColor.GOLD + "[팀] " + ChatColor.WHITE + m); }
    private boolean playerOnly(CommandSender s) { if (s instanceof Player) return true; msg(s,"플레이어만 사용할 수 있습니다."); return false; }
    private String colorName(ChatColor c) { for (var e:COLORS.entrySet()) if(e.getValue()==c) return e.getKey(); return "흰색"; }

    private void updateName(Player p) {
        TeamData td=teamOf(p.getUniqueId());
        Scoreboard board=Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team t : board.getTeams()) if(t.getName().startsWith("ts")) t.removeEntry(p.getName());
        if(td==null) { p.setDisplayName(p.getName()); p.setPlayerListName(p.getName()); return; }
        String id=key(td.name);
        Team st=board.getTeam(id); if(st==null) st=board.registerNewTeam(id);
        st.setColor(td.color); st.addEntry(p.getName());
        p.setDisplayName(td.color + p.getName() + ChatColor.RESET);
        p.setPlayerListName(td.color + p.getName() + ChatColor.RESET);
    }
    private void refreshOnlineNames(){ for(Player p:Bukkit.getOnlinePlayers()) updateName(p); }
    private ItemStack menuItem(Material material,String title,List<String> lore) {
        ItemStack item=new ItemStack(material); ItemMeta meta=item.getItemMeta();
        meta.setDisplayName(title); meta.setLore(lore); item.setItemMeta(meta); return item;
    }
    private void openColorMenu(Player p) {
        Inventory inv=Bukkit.createInventory(null,27,COLOR_GUI); int slot=10;
        for(var e:COLORS.entrySet()) {
            Material mat=switch(e.getKey()) {
                case "빨강" -> Material.RED_WOOL; case "주황","금색" -> Material.ORANGE_WOOL;
                case "노랑" -> Material.YELLOW_WOOL; case "초록" -> Material.LIME_WOOL;
                case "청록" -> Material.CYAN_WOOL; case "파랑" -> Material.BLUE_WOOL;
                case "보라","분홍" -> Material.PURPLE_WOOL; case "회색" -> Material.GRAY_WOOL;
                case "검정" -> Material.BLACK_WOOL; default -> Material.WHITE_WOOL;
            };
            inv.setItem(slot,menuItem(mat,e.getValue()+e.getKey(),List.of("클릭하여 팀 이름표 색상으로 설정")));
            slot++; if(slot==17)slot=19;
        }
        p.openInventory(inv);
    }
    private void openTeamList(Player p) {
        Inventory inv=Bukkit.createInventory(null,54,LIST_GUI);
        List<TeamData> sorted=new ArrayList<>(teams.values());
        sorted.sort(Comparator.comparing(t->t.name.toLowerCase(Locale.ROOT)));
        int slot=0;
        for(TeamData t:sorted) {
            if(slot>=54)break;
            OfflinePlayer leaderPlayer=Bukkit.getOfflinePlayer(t.leader);
            ItemStack head=new ItemStack(Material.PLAYER_HEAD); SkullMeta meta=(SkullMeta)head.getItemMeta();
            meta.setOwningPlayer(leaderPlayer);
            meta.setDisplayName(t.color+"[팀장] "+(leaderPlayer.getName()==null?"알 수 없음":leaderPlayer.getName())+ChatColor.RESET);
            List<String> lore=new ArrayList<>(); lore.add(ChatColor.YELLOW+"팀 이름: "+t.name);
            lore.add(ChatColor.GRAY+"소속 팀원:");
            for(UUID id:t.members) { OfflinePlayer member=Bukkit.getOfflinePlayer(id);
                lore.add(ChatColor.WHITE+"- "+(member.getName()==null?id.toString():member.getName())); }
            lore.add(ChatColor.GREEN+"인원: "+t.members.size()+"명");
            meta.setLore(lore); head.setItemMeta(meta); inv.setItem(slot++,head);
        }
        if(sorted.isEmpty()) inv.setItem(22,menuItem(Material.BARRIER,ChatColor.RED+"생성된 팀이 없습니다.",List.of()));
        p.openInventory(inv);
    }
    private void saveData() {
        getConfig().set("teams", null);
        for(TeamData t:teams.values()) {
            String path="teams."+t.name;
            getConfig().set(path+".leader",t.leader.toString());
            getConfig().set(path+".color",t.color.name());
            getConfig().set(path+".members",t.members.stream().map(UUID::toString).toList());
            getConfig().set(path+".allies",new ArrayList<>(t.allies));
        }
        saveConfig();
    }
    private void loadData() {
        ConfigurationSection root=getConfig().getConfigurationSection("teams"); if(root==null)return;
        for(String n:root.getKeys(false)) try {
            ConfigurationSection s=root.getConfigurationSection(n); UUID leader=UUID.fromString(s.getString("leader"));
            TeamData t=new TeamData(n,leader); t.color=ChatColor.valueOf(s.getString("color","WHITE"));
            t.members.clear(); for(String id:s.getStringList("members")) { UUID u=UUID.fromString(id); t.members.add(u); playerTeams.put(u,n.toLowerCase(Locale.ROOT)); }
            t.allies.addAll(s.getStringList("allies")); teams.put(n.toLowerCase(Locale.ROOT),t);
        } catch(Exception ex){ getLogger().warning("팀 데이터 로드 실패: "+n); }
    }
    private void leave(Player p, boolean announce) {
        TeamData t=teamOf(p.getUniqueId()); if(t==null)return;
        t.members.remove(p.getUniqueId()); playerTeams.remove(p.getUniqueId()); betrayers.remove(p.getUniqueId()); updateName(p);
        if(announce) msg(p,"팀에서 나왔습니다. 이름표 색이 기본으로 돌아왔습니다.");
        if(t.members.isEmpty()) { teams.remove(t.name.toLowerCase(Locale.ROOT)); for(TeamData x:teams.values()) x.allies.remove(t.name.toLowerCase(Locale.ROOT)); }
        else if(t.leader.equals(p.getUniqueId())) t.leader=t.members.iterator().next();
        saveData();
    }
    private boolean leader(Player p, TeamData t) { if(!t.leader.equals(p.getUniqueId())) { msg(p,"팀장만 할 수 있습니다."); return false; } return true; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("팀생성")) {
            if(!playerOnly(sender))return true; Player p=(Player)sender;
            if(args.length<1){msg(p,"사용법: /팀생성 <팀이름>");return true;}
            if(teamOf(p.getUniqueId())!=null){msg(p,"이미 팀에 속해 있습니다.");return true;}
            String name=String.join(" ",args); String k=name.toLowerCase(Locale.ROOT);
            if(teams.containsKey(k)){msg(p,"이미 존재하는 팀 이름입니다.");return true;}
            if(name.length()>16){msg(p,"팀 이름은 16자 이하로 해주세요.");return true;}
            TeamData t=new TeamData(name,p.getUniqueId()); teams.put(k,t); playerTeams.put(p.getUniqueId(),k);
            updateName(p); saveData(); msg(p,"팀 '"+name+"' 생성 완료! 기본 색상은 흰색입니다. /팀 색상 <색> 으로 변경하세요."); return true;
        }
        if(command.getName().equalsIgnoreCase("팀")) {
            if(!playerOnly(sender))return true; Player p=(Player)sender;
            if(args.length==0){msg(p,"/팀 초대 <플레이어> | /팀 초대 수락 | /팀 나가기 | /팀 색상 <색>");return true;}
            if(args[0].equals("초대")) {
                if(args.length>=2 && args[1].equals("수락")) {
                    String invited=teamInvites.remove(p.getUniqueId()); if(invited==null){msg(p,"받은 팀 초대가 없습니다.");return true;}
                    if(teamOf(p.getUniqueId())!=null){msg(p,"이미 팀에 속해 있습니다.");return true;}
                    TeamData t=teams.get(invited); if(t==null){msg(p,"초대한 팀이 더 이상 존재하지 않습니다.");return true;}
                    t.members.add(p.getUniqueId()); playerTeams.put(p.getUniqueId(),invited); updateName(p); saveData();
                    msg(p,"팀 '"+t.name+"'에 가입했습니다."); for(UUID u:t.members){Player m=Bukkit.getPlayer(u);if(m!=null&&!m.equals(p))msg(m,p.getName()+"님이 팀에 가입했습니다.");} return true;
                }
                if(args.length<2){msg(p,"사용법: /팀 초대 <플레이어>");return true;}
                TeamData t=teamOf(p.getUniqueId()); if(t==null){msg(p,"먼저 팀을 만드세요.");return true;} if(!leader(p,t))return true;
                Player target=Bukkit.getPlayerExact(args[1]); if(target==null){msg(p,"접속 중인 플레이어만 초대할 수 있습니다.");return true;}
                if(teamOf(target.getUniqueId())!=null){msg(p,"상대가 이미 팀에 속해 있습니다.");return true;}
                teamInvites.put(target.getUniqueId(),t.name.toLowerCase(Locale.ROOT));
                msg(target,"'"+t.name+"' 팀 초대가 왔습니다. /팀 초대 수락 으로 가입하세요."); msg(p,target.getName()+"님을 초대했습니다."); return true;
            }
            if(args[0].equals("이름표")) { TeamData t=teamOf(p.getUniqueId()); if(t==null){msg(p,"팀에 속해 있지 않습니다.");return true;} if(leader(p,t))openColorMenu(p); return true; }
            if(args[0].equals("목록")) { openTeamList(p); return true; }
            if(args[0].equals("나가기")) { if(teamOf(p.getUniqueId())==null)msg(p,"속한 팀이 없습니다."); else leave(p,true); return true; }
            if(args[0].equals("색상")) {
                TeamData t=teamOf(p.getUniqueId()); if(t==null){msg(p,"팀에 속해 있지 않습니다.");return true;} if(!leader(p,t))return true;
                if(args.length<2){msg(p,"색상: "+String.join(", ",COLORS.keySet()));return true;}
                ChatColor c=COLORS.get(args[1]); if(c==null){msg(p,"가능한 색상: "+String.join(", ",COLORS.keySet()));return true;}
                t.color=c; for(UUID u:t.members){Player m=Bukkit.getPlayer(u);if(m!=null)updateName(m);} saveData(); msg(p,"팀 이름표 색상을 "+args[1]+"(으)로 변경했습니다.");return true;
            }
            msg(p,"/팀 초대 <플레이어> | /팀 초대 수락 | /팀 나가기 | /팀 색상 <색>"); return true;
        }
        if(command.getName().equalsIgnoreCase("팀장")) {
            if(!playerOnly(sender))return true; Player p=(Player)sender;
            TeamData t=teamOf(p.getUniqueId()); if(t==null){msg(p,"팀에 속해 있지 않습니다.");return true;}
            if(!leader(p,t))return true;
            int offset=args.length>0&&args[0].equals("임명")?1:0;
            if(args.length<=offset){msg(p,"사용법: /팀장 임명 <플레이어이름>");return true;}
            Player target=Bukkit.getPlayerExact(args[offset]);
            if(target==null||teamOf(target.getUniqueId())!=t){msg(p,"같은 팀에 속한 접속 중인 플레이어만 팀장으로 임명할 수 있습니다.");return true;}
            t.leader=target.getUniqueId(); saveData();
            msg(p,target.getName()+"님을 새 팀장으로 임명했습니다."); msg(target,"당신이 팀장으로 임명되었습니다."); return true;
        }
        if(command.getName().equalsIgnoreCase("배신")) {
            if(!playerOnly(sender))return true; Player p=(Player)sender; if(teamOf(p.getUniqueId())==null){msg(p,"팀에 속해 있지 않습니다.");return true;}
            if(betrayers.add(p.getUniqueId()))msg(p,"배신 모드 ON: 같은 팀원을 공격할 수 있습니다. 이름표 색은 유지됩니다.");
            else {betrayers.remove(p.getUniqueId());msg(p,"배신 모드 OFF: 같은 팀원을 공격할 수 없습니다.");} return true;
        }
        if(command.getName().equalsIgnoreCase("동맹")) {
            if(!playerOnly(sender))return true; Player p=(Player)sender; TeamData own=teamOf(p.getUniqueId());
            if(own==null){msg(p,"팀에 속해 있지 않습니다.");return true;}
            if(args.length==0){msg(p,"/동맹 <팀이름> | /동맹 수락 | /동맹 거절 | /동맹 끝");return true;}
            if(args[0].equals("수락")||args[0].equals("거절")) {
                if(!leader(p,own))return true; String from=allianceInvites.remove(own.name.toLowerCase(Locale.ROOT));
                if(from==null){msg(p,"받은 동맹 신청이 없습니다.");return true;}
                if(args[0].equals("수락")) { TeamData other=teams.get(from); if(other!=null){own.allies.add(from);other.allies.add(own.name.toLowerCase(Locale.ROOT));saveData();msg(p,"동맹을 수락했습니다."); for(UUID u:other.members){Player m=Bukkit.getPlayer(u);if(m!=null)msg(m,"'"+own.name+"' 팀이 동맹을 수락했습니다.");}} }
                else msg(p,"동맹 신청을 거절했습니다."); return true;
            }
            if(args[0].equals("끝")) {
                if(!leader(p,own))return true; if(args.length<2) {
                    if(own.allies.isEmpty()){msg(p,"끝낼 동맹이 없습니다.");return true;}
                    for(String ally : new HashSet<>(own.allies)){TeamData other=teams.get(ally); if(other!=null)other.allies.remove(own.name.toLowerCase(Locale.ROOT));}
                    own.allies.clear(); saveData(); msg(p,"모든 동맹을 끝냈습니다."); return true;
                }
                TeamData other=teams.get(args[1].toLowerCase(Locale.ROOT)); if(other==null||!own.allies.remove(args[1].toLowerCase(Locale.ROOT))){msg(p,"해당 팀과 동맹이 아닙니다.");return true;}
                other.allies.remove(own.name.toLowerCase(Locale.ROOT)); saveData();msg(p,"동맹을 끝냈습니다.");return true;
            }
            if(!leader(p,own))return true; String targetName=String.join(" ",args); String k=targetName.toLowerCase(Locale.ROOT); TeamData other=teams.get(k);
            if(other==null){msg(p,"해당 팀을 찾을 수 없습니다.");return true;} if(other==own){msg(p,"자기 팀과는 동맹할 수 없습니다.");return true;}
            allianceInvites.put(k,own.name.toLowerCase(Locale.ROOT));
            for(UUID u:other.members){Player m=Bukkit.getPlayer(u);if(m!=null)msg(m,"'"+own.name+"' 팀이 동맹을 신청했습니다. 팀장이 /동맹 수락 또는 /동맹 거절로 응답하세요.");}
            msg(p,"'"+other.name+"' 팀에 동맹을 신청했습니다."); return true;
        }
        return true;
    }

    @EventHandler public void onDamage(EntityDamageByEntityEvent e) {
        if(!(e.getEntity() instanceof Player victim))return;
        Player attacker=null; Entity damager=e.getDamager();
        if(damager instanceof Player p) attacker=p;
        else if(damager instanceof Projectile projectile && projectile.getShooter() instanceof Player p) attacker=p;
        if(attacker==null)return;
        TeamData a=teamOf(attacker.getUniqueId()), v=teamOf(victim.getUniqueId());
        if(a==null||v==null)return;
        if(a.name.equalsIgnoreCase(v.name)) { if(!betrayers.contains(attacker.getUniqueId()))e.setCancelled(true); }
        else if(a.allies.contains(v.name.toLowerCase(Locale.ROOT))) e.setCancelled(true);
    }
    @EventHandler public void onInventoryClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player p))return;
        String title=e.getView().getTitle();
        if(!title.equals(COLOR_GUI)&&!title.equals(LIST_GUI))return;
        e.setCancelled(true);
        if(title.equals(COLOR_GUI)&&e.getCurrentItem()!=null&&e.getCurrentItem().hasItemMeta()) {
            TeamData t=teamOf(p.getUniqueId());
            if(t==null||!t.leader.equals(p.getUniqueId())){p.closeInventory();return;}
            String display=ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
            ChatColor chosen=COLORS.get(display); if(chosen==null)return;
            t.color=chosen;
            for(UUID id:t.members){Player member=Bukkit.getPlayer(id);if(member!=null)updateName(member);}
            saveData(); msg(p,"팀 이름표 색상을 "+display+"(으)로 변경했습니다."); p.closeInventory();
        }
    }
    @EventHandler public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) { updateName(e.getPlayer()); }
}
