package server;

/**
 * @author EMTI
 */
import EMTI.SystemMetrics;
import boss.AnTromManager;
import boss.BossManager;
import boss.BrolyManager;
import boss.GasDestroyManager;
import boss.OtherBossManager;
import boss.RedRibbonHQManager;
import boss.SnakeWayManager;
import boss.TreasureUnderSeaManager;
import boss.TrungThuEventManager;
import consts.ConstNpc;
import data.DataGame;
import item.Item;

import java.util.List;

import minigame.LuckyNumber.LuckyNumber;
import models.GiftCode.GiftCodeManager;
import models.ShenronEvent.ShenronEvent;
import models.ShenronEvent.ShenronEventManager;
import network.SessionManager;
import player.Pet;
import player.Player;
import player.badges.BadgesData;
import services.InventoryService;
import services.ItemService;
import services.NpcService;
import services.PetService;
import services.Service;
import services.SkillService;
import services.TaskService;
import services.func.ChangeMapService;
import services.func.Input;
import skill.Skill;

public class Command {

    private static Command instance;

    public static Command gI() {
        if (instance == null) {
            instance = new Command();
        }
        return instance;
    }

    public void chat(Player player, String text) {
        if (!check(player, text)) {
            Service.gI().chat(player, text);
        }
    }

    public boolean check(Player player, String text) {
        if (player.isAdmin()) {
            if (text.equals("giftcode")) {
                GiftCodeManager.gI().checkInfomationGiftCode(player);
                return true;
            } else if (text.equals("mapboss")) {
                BossManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("mapbroly")) {
                BrolyManager.gI().showListBoss(player);
                return true;
            }  else if (text.equals("mapantrom")) {
               AnTromManager.gI().showListBoss(player);
                return true;
            }else if (text.equals("mapboss2")) {
                OtherBossManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("mapdt")) {
                RedRibbonHQManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("mapbdkb")) {
                TreasureUnderSeaManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("mapcdrd")) {
                SnakeWayManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("mapkghd")) {
                GasDestroyManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("maptrungthu")) {
                TrungThuEventManager.gI().showListBoss(player);
                return true;
            } else if (text.equals("hsk")) {
                Service.gI().releaseCooldownSkill(player);
                return true;
            } else if (text.equals("reloaddata")) {
                DataGame.reloadVersions();
                Service.gI().sendThongBao(player, "Đã reload data versions từ config!");
                return true;
            } else if (text.equals("reloadtambao")) {
                services.TamBaoService.gI().reloadPools();
                Service.gI().sendThongBao(player, "Đã reload dữ liệu Tầm Bảo từ database!");
                return true;
            } else if (text.equals("reloadnaptuan")) {
                services.WeeklyRewardService.gI().reloadWeeklyRewards();
                Service.gI().sendThongBao(player, "Đã reload dữ liệu Quà Nạp Tuần từ database!");
                return true;
            } else if (text.equals("resetnaptuan")) {
                services.WeeklyRewardService.gI().resetWeeklyRewards();
                Service.gI().sendThongBao(player, "Đã reset quà nạp tuần - tất cả player có thể nhận lại!");
                return true;
            } else if (text.startsWith("sp")) {
                try {
                    long power = Long.parseLong(text.replaceAll("sp", ""));
                    Service.gI().addSMTN(player, (byte) 2, power, false);
                    Service.gI().sendThongBao(player, "Đã tăng " + power + " sức mạnh!");
                    return true;
                } catch (Exception e) {
                    Service.gI().sendThongBao(player, "Lỗi lệnh sp: " + e.getMessage());
                }
            } else if (text.equals("battu")) {
                if (player.isBattu) {
                    player.isBattu = false;
                } else {
                    player.isBattu = true;
                }
                Service.gI().sendThongBao(player, "Bất tử" + (player.isBattu ? ": ON" : ": OFF"));
                return true;
            } else if (text.startsWith("dt")) {
                try {
                    if (player.pet == null) {
                        Service.gI().sendThongBao(player, "Bạn chưa có đệ tử!");
                        return true;
                    }
                    long power = Long.parseLong(text.replaceAll("dt", ""));
                    Service.gI().addSMTN(player.pet, (byte) 2, power, false);
                    Service.gI().sendThongBao(player, "Đã tăng " + power + " sức mạnh cho đệ tử!");
                    return true;
                } catch (Exception e) {
                    Service.gI().sendThongBao(player, "Lỗi lệnh dt: " + e.getMessage());
                }
            } else if (text.equals("test")) {
                switch (player.gender) {
                    case 0 ->
                        SkillService.gI().learSkillSpecial(player, Skill.SUPER_KAME, 1);
                    case 2 ->
                        SkillService.gI().learSkillSpecial(player, Skill.LIEN_HOAN_CHUONG, 1);
                    default ->
                        SkillService.gI().learSkillSpecial(player, Skill.MA_PHONG_BA, 1);
                }
                return true;
            } else if (text.equals("test2")) {
                switch (player.gender) {
                    case 0 -> {
                        SkillService.gI().learSkillSpecial(player, Skill.PHAN_THAN, 6);
                    }
                    case 2 -> {
                        SkillService.gI().learSkillSpecial(player, Skill.PHAN_THAN, 6);
                    }
                    default -> {
                        SkillService.gI().learSkillSpecial(player, Skill.PHAN_THAN, 6);
                    }
                }
                return true;
            } else if (text.equals("dragon")) {
                ShenronEvent shenron = new ShenronEvent();
                shenron.setPlayer(player);
                ShenronEventManager.gI().add(shenron);
                player.shenronEvent = shenron;
                shenron.setZone(player.zone);
                shenron.activeShenron(true, ShenronEvent.DRAGON_EVENT);
                shenron.sendWhishesShenron();
                return true;
            } else if (text.equals("admin")) {
                NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_ADMIN, -1, "|0|Time start: " + ServerManager.timeStart + "\nClients: " + Client.gI().getPlayers().size() + " người chơi\n Sessions: " + SessionManager.gI().getNumSession() + "\nThreads: " + Thread.activeCount() + " luồng" + "\n" + SystemMetrics.ToString(),
                        "Ngọc rồng", "Đệ tử", "Bảo trì", "Tìm kiếm\nngười chơi", "Boss", "Call Broly","Buff VND","Buff\nhộp thư", "Buff toàn\nserver", "Đóng");
                return true;
            } else if (text.equals("daucatmoi")) {
                for (int i = 0; i < 10; i++) {
                    ServerNotify.gI().notify("BOSS ADMIN vừa xuất hiện tại nhà anh ấy");
                }
                return true;
            } else if (text.equals("botl")) {
                java.util.Map<Integer, java.util.List<Player>> byMap = new java.util.HashMap<>();
                for (Player p : Client.gI().getPlayers()) {
                    if (p != null && p.isBot && p.zone != null) {
                        byMap.computeIfAbsent(p.zone.map.mapId, k -> new java.util.ArrayList<>()).add(p);
                    }
                }
                if (byMap.isEmpty()) {
                    Service.gI().sendThongBao(player, "Không có bot nào đang hoạt động");
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (java.util.Map.Entry<Integer, java.util.List<Player>> e : byMap.entrySet()) {
                    int mapId = e.getKey();
                    String mapName = e.getValue().get(0).zone.map.mapName;
                    sb.append("Map ").append(mapId).append(" - ").append(mapName)
                      .append(": ").append(e.getValue().size()).append(" bot\n");
                    int limit = Math.min(5, e.getValue().size());
                    for (int i = 0; i < limit; i++) {
                        Player bp = e.getValue().get(i);
                        sb.append("  - ").append(bp.name)
                          .append(" (zone ").append(bp.zone.zoneId)
                          .append(", x=").append(bp.location.x)
                          .append(", y=").append(bp.location.y).append(")\n");
                    }
                }
                Service.gI().sendThongBao(player, sb.toString());
                return true;
            } else if (text.equals("resetboss")) {
                int n = BossManager.gI().resetAllBosses();
                Service.gI().sendThongBao(player, "Đã reset " + n + " boss");
                return true;
            } else if (text.startsWith("botname ")) {
                try {
                    String wanted = text.substring("botname ".length()).trim();
                    if (wanted.isEmpty()) {
                        Service.gI().sendThongBao(player, "Tên không hợp lệ");
                        return true;
                    }
                    Client.gI().createBotWithName((server.io.MySession) player.getSession(), wanted);
                    Service.gI().sendThongBao(player, "Đã tạo bot tên: " + wanted);
                    return true;
                } catch (Exception e) {
                    Service.gI().sendThongBao(player, "Lỗi tạo bot theo tên: " + e.getMessage());
                    return true;
                }
            } else if (text.startsWith("bot")) {
                try {
                    // bot_create [count]
                    int count = 1;
                    String[] sp = text.split(" ");
                    if (sp.length > 1) count = Math.max(1, Integer.parseInt(sp[1]));
                    for (int i = 0; i < count; i++) {
                        Client.gI().createBot((server.io.MySession) player.getSession());
                    }
                    Service.gI().sendThongBao(player, "Đã tạo " + count + " bot");
                    return true;
                } catch (Exception e) {
                    Service.gI().sendThongBao(player, "Lỗi tạo bot: " + e.getMessage());
                    return true;
                }
            } else if (text.equals("botclear")) {
                Client.gI().clearBots();
                Service.gI().sendThongBao(player, "Đã xoá bot");
                return true;
            } else if (text.startsWith("m ")) {
                int mapId = Integer.parseInt(text.replace("m ", ""));
                ChangeMapService.gI().changeMapInYard(player, mapId, -1, -1);
                return true;
            }
            if (text.startsWith("dmg")) {
                try {
                    long dameg = Integer.parseInt(text.replaceAll("dmg", ""));
                    player.nPoint.dameg = dameg;
                    Service.gI().point(player);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (text.startsWith("hpg")) {
                try {
                    long hpg = Integer.parseInt(text.replaceAll("hpg", ""));
                    player.nPoint.hpg = hpg;
                    Service.gI().point(player);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (text.startsWith("mpg")) {
                try {
                    long mpg = Integer.parseInt(text.replaceAll("mpg", ""));
                    player.nPoint.mpg = mpg;
                    Service.gI().point(player);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (text.startsWith("defg")) {
                try {
                    int defg = Integer.parseInt(text.replaceAll("defg", ""));
                    player.nPoint.defg = defg;
                    Service.gI().point(player);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (text.startsWith("crg")) {
                try {
                    int critg = Integer.parseInt(text.replaceAll("crg", ""));
                    player.nPoint.critg = critg;
                    Service.gI().point(player);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (text.startsWith("ntask")) {
                try {
                    int idTask = Integer.parseInt(text.replaceAll("ntask", ""));
                    player.playerTask.taskMain.id = idTask - 1;
                    player.playerTask.taskMain.index = 0;
                    TaskService.gI().sendNextTaskMain(player);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (text.startsWith("badges")) {
                int idBadges = Integer.parseInt(text.replaceAll("badges_", ""));
                player.badges.idBadges = idBadges;
            }
            if (text.startsWith("kq")) {
                Service.gI().sendThongBao(player, "Kết quả Lucky Round tiếp theo là: " + LuckyNumber.RESULT);
                return true;
            }
            if (text.startsWith("danhhieu")) {
                int idGender = Integer.parseInt(text.replaceAll("danhhieu_", ""));
                BadgesData data = new BadgesData(player, idGender, 5);
                return true;
            }
            if (text.startsWith("gender")) {
                byte idGender = Byte.parseByte(text.replaceAll("gender_", ""));
                player.gender = idGender;
                return true;
            }
            if (text.startsWith("i")) {
                String[] parts = text.split(" ");
                if (parts.length >= 3) {
                    short id = Short.parseShort(parts[1]);
                    int quantity = Integer.parseInt(parts[2]);
                    Item item = ItemService.gI().createNewItem(id, quantity);
                    List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) id);
                    if (!ops.isEmpty()) {
                        item.itemOptions = ops;
                    }
                    InventoryService.gI().addItemBag(player, item);
                    InventoryService.gI().sendItemBag(player);
                    Service.gI().sendThongBao(player, "GET " + item.template.name + " [" + item.template.id + "] SUCCESS !");
                    return true;
                } else {
                    Service.gI().sendThongBao(player, "Lỗi");
                    return true;
                }
            }
//            }             else if (text.startsWith("i ")) {
//                            int itemId = Integer.parseInt(text.replace("i ", ""));
//                            Item item = ItemService.gI().createNewItem(((short) itemId));
//                            List<Item.ItemOption> ops = ItemService.gI().getListOptionItemShop((short) itemId);
//                            if (!ops.isEmpty()) {
//                                item.itemOptions = ops;
//                            }
//                            InventoryService.gI().addItemBag(player, item);
//                            InventoryService.gI().sendItemBag(player);
//                            Service.gI().sendThongBao(player, "GET " + item.template.name + " [" + item.template.id + "] SUCCESS !");
//                            return true;
//                        }
            else if (text.equals("item")) {
                Input.gI().createFormGiveItem(player);
                return true;
            } else if (text.equals("getitem")) {
                Input.gI().createFormGetItem(player);
                return true;
            } else if (text.equals("buffall")) {
                Input.gI().createFormBuffAllServer(player);
                return true;
            } else if (text.equals("d")) {
                Service.gI().setPos(player, player.location.x, player.location.y + 10);
                return true;
            }
        }
        if (text.startsWith("ten con la ")) {
            PetService.gI().changeNamePet(player, text.replaceAll("ten con la ", ""));
        }

        if (player.pet != null) {
            switch (text) {
                case "di theo", "follow" ->
                    player.pet.changeStatus(Pet.FOLLOW);
                case "bao ve", "protect" ->
                    player.pet.changeStatus(Pet.PROTECT);
                case "tan cong", "attack" ->
                    player.pet.changeStatus(Pet.ATTACK);
                case "ve nha", "go home" ->
                    player.pet.changeStatus(Pet.GOHOME);
                case "bien hinh" ->
                    player.pet.transform();
            }
        }
        return false;
    }
}
