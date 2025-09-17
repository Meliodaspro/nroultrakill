package server;

/*
 *
 *
 * @author EMTI
 */
import EMTI.Functions;
import jdbc.DBConnecter;
import jdbc.daos.PlayerDAO;
import map.ItemMap;
import player.Player;
import network.server.EmtiSessionManager;
import network.session.ISession;
import server.io.MySession;
import services.func.ChangeMapService;
import services.func.SummonDragon;
import services.func.TransactionService;
import services.NgocRongNamecService;
import utils.Logger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.DragonNamecWar.TranhNgoc;
import services.func.SummonDragonNamek;
import utils.Util;
import services.Service;
import services.InventoryService;
import services.ItemService;
import item.Item;

public class Client implements Runnable {

    private static Client instance;

    private final Map<Long, Player> players_id = new HashMap<>();
    private final Map<Integer, Player> players_userId = new HashMap<>();
    private final Map<String, Player> players_name = new HashMap<>();
    private final List<Player> players = new ArrayList<>();

    private Client() {
        utils.Threading.runLongLived(this);
    }

    public List<Player> getPlayers() {
        return this.players;
    }

    public static Client gI() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    public void put(Player player) {
        if (!players_id.containsKey(player.id)) {
            this.players_id.put(player.id, player);
        }
        if (!players_name.containsValue(player)) {
            this.players_name.put(player.name, player);
        }
        if (!players_userId.containsValue(player)) {
            if (player.getSession() != null)
                this.players_userId.put(player.getSession().userId, player);
        }
        if (!players.contains(player)) {
            this.players.add(player);
        }

    }

    public void createBot(server.io.MySession s) {
        try {
            Player pl = new Player();
            pl.setSession(s);
            pl.id = System.currentTimeMillis();
            pl.name = generateUniqueName(null, null, null);
            pl.gender = (byte) utils.Util.nextInt(0, 2);
            pl.isBot = true;
            pl.isBoss = false;
            pl.isPet = false;
            pl.isPlayer = false;
            // Chọn mode hành vi: 0 đứng im/chat, 1 săn mob
            pl.botMode = (byte) (Util.isTrue(1, 2) ? 1 : 0);
            pl.nPoint.power = utils.Util.nextLong(2000L, 180000000000L);
            pl.nPoint.hpMax = Math.max(2000, utils.Util.nextInt(2000, 500000));
            pl.nPoint.hpg = pl.nPoint.hpMax;
            pl.nPoint.hp = Math.max(1, pl.nPoint.hpMax / 2);
            pl.nPoint.mpMax = Math.max(2000, utils.Util.nextInt(2000, 500000));
            pl.nPoint.mpg = pl.nPoint.mpMax;
            pl.nPoint.stamina = 32000;
            pl.nPoint.dameg = utils.Util.nextInt(500, 2000);
            pl.nPoint.calPoint();
            // tạo inventory và outfit
            pl.inventory = new player.Inventory();
            while (pl.inventory.itemsBody.size() < 13) pl.inventory.itemsBody.add(ItemService.gI().createItemNull());
            // Outfit bot đa dạng theo giới tính (random nhiều bộ)
            try {
                short[][] pool;
                if (pl.gender == 0) {
                    pool = new short[][]{
                        {2180, 2183, 2184}, // set yêu cầu
                        {870, 871, 872},    // Porata cấp 2
                        {383, 384, 385},    // Porata cấp 1 (đất/xayda)
                        {1457, 1459, 1460},
                        {1467, 1470, 1471},
                        {1455, 1450, 1451},
                        {1518, 1521, 1522},
                        {1543, 1544, 1545},
                        {1546, 1548, 1549},
                        {1553, 1556, 1557},
                        {1564, 1567, 1568},
                        {1569, 1570, 1571},
                        {1575, 1576, 1577},
                        {1578, 1581, 1582},
                        {1583, 1584, 1585},
                        {1586, 1587, 1588},
                        {1592, 1593, 1594},
                        {1353, 1354, 1355},
                        {1362, 1363, 1364},
                        {1359, 1360, 1361},
                        {1380, 1381, 1382},
                        {553, 551, 552},
                        {1380, 1381, 1382},
                        {1395, 1396, 1397},
                        {1404, 1405, 1406}

                    };
                } else if (pl.gender == 1) {
                    pool = new short[][]{
                        {2190, 2193, 2194}, // set yêu cầu
                        {873, 874, 875},    // Porata cấp 2
                        {391, 392, 393},    // Porata cấp 1 (namec)
                        {1457, 1459, 1460},
                        {1467, 1470, 1471},
                        {1455, 1450, 1451},
                        {1518, 1521, 1522},
                        {1543, 1544, 1545},
                        {1546, 1548, 1549},
                        {1553, 1556, 1557},
                        {1564, 1567, 1568},
                        {1569, 1570, 1571},
                        {1575, 1576, 1577},
                         {1578, 1581, 1582},
                        {1583, 1584, 1585},
                        {1586, 1587, 1588},
                        {1592, 1593, 1594},
                        {1353, 1354, 1355},
                        {1362, 1363, 1364},
                        {1359, 1360, 1361},
                        {1380, 1381, 1382},
                        {553, 551, 552},
                        {1380, 1381, 1382},
                        {1395, 1396, 1397},
                        {1404, 1405, 1406}
                    };
                } else {
                    pool = new short[][]{
                        {2185, 2188, 2189}, // set yêu cầu
                        {867, 868, 869},    // Porata cấp 2 (namec)
                        {1457, 1459, 1460},
                        {1467, 1470, 1471},
                        {1455, 1450, 1451},
                        {1518, 1521, 1522},
                        {1543, 1544, 1545},
                        {1546, 1548, 1549},
                        {1553, 1556, 1557},
                        {1564, 1567, 1568},
                        {1569, 1570, 1571},
                        {1575, 1576, 1577},
                        {1578, 1581, 1582},
                        {1583, 1584, 1585},
                        {1586, 1587, 1588},
                        {1592, 1593, 1594},
                        {1353, 1354, 1355},
                        {1362, 1363, 1364},
                        {1359, 1360, 1361},
                        {1380, 1381, 1382},
                        {553, 551, 552},
                        {1380, 1381, 1382},
                        {1395, 1396, 1397},
                        {1404, 1405, 1406}
                    };
                }
                short[] set = pool[utils.Util.nextInt(pool.length)];
                pl.botHead = set[0];
                pl.botBody = set[1];
                pl.botLeg = set[2];
            } catch (Exception ignored) {}
            // cấp bộ kỹ năng cơ bản theo giới tính
            try {
                int[] skillsArr = pl.gender == 0 ? new int[]{0, 1, 6, 9}
                        : pl.gender == 1 ? new int[]{2, 3, 7}
                        : new int[]{4, 5, 8};
                for (int sid : skillsArr) {
                    pl.playerSkill.skills.add(utils.SkillUtil.createSkill(sid, 1));
                }
                if (!pl.playerSkill.skills.isEmpty()) {
                    pl.playerSkill.skillSelect = pl.playerSkill.skills.get(0);
                }
            } catch (Exception ignored) {}
            // chọn map ngẫu nhiên toàn server (bỏ các map cấm & map đặc biệt)
            map.Zone zoneJoin = null;
            for (int tries = 0; tries < 50 && zoneJoin == null; tries++) {
                map.Map m = Manager.MAPS.get(utils.Util.nextInt(Manager.MAPS.size()));
                if (isBannedMap(m.mapId)) continue; // tránh map sự kiện/phó bản
                zoneJoin = services.MapService.gI().getMapWithRandZone(m.mapId);
            }
            if (zoneJoin == null) return;
            // chọn điểm spawn an toàn: upper-half của map
            int triesXY = 0;
            int safeY = 100;
            do {
                pl.location.x = utils.Util.nextInt(100, Math.max(120, zoneJoin.map.mapWidth - 100));
                safeY = zoneJoin.map.yPhysicInTop(pl.location.x, 24);
                triesXY++;
            } while (triesXY < 20 && safeY > zoneJoin.map.mapHeight / 2);
            if (safeY > zoneJoin.map.mapHeight / 2) {
                // vẫn không an toàn: kẹp lên nửa trên
                safeY = Math.min(safeY, (zoneJoin.map.mapHeight / 2) - 10);
            }
            pl.location.y = Math.max(24, safeY);
            services.func.ChangeMapService.gI().goToMap(pl, zoneJoin);
            zoneJoin.load_Me_To_Another(pl);
            this.put(pl);
            // Không start thread riêng cho bot, update trong Zone.update()
            // Chat định kỳ: đã chuyển sang Player.updateBot()
        } catch (Exception ignored) {}
    }

    public void createBotWithName(server.io.MySession s, String wantedName) {
        try {
            Player pl = new Player();
            pl.setSession(s);
            pl.id = System.currentTimeMillis();
            pl.name = ensureUniqueName(wantedName);
            pl.gender = (byte) utils.Util.nextInt(0, 2);
            pl.isBot = true;
            pl.isBoss = false;
            pl.isPet = false;
            pl.isPlayer = false;
            pl.botMode = (byte) (Util.isTrue(1, 2) ? 1 : 0);
            pl.nPoint.power = utils.Util.nextInt(2000, 20000000);
            pl.nPoint.hpMax = Math.max(2000, utils.Util.nextInt(2000, 500000));
            pl.nPoint.hpg = pl.nPoint.hpMax;
            pl.nPoint.hp = Math.max(1, pl.nPoint.hpMax / 2);
            pl.nPoint.mpMax = Math.max(2000, utils.Util.nextInt(2000, 500000));
            pl.nPoint.mpg = pl.nPoint.mpMax;
            pl.nPoint.stamina = 32000;
            pl.nPoint.dameg = utils.Util.nextInt(500, 2000);
            pl.nPoint.calPoint();
            pl.inventory = new player.Inventory();
            while (pl.inventory.itemsBody.size() < 13) pl.inventory.itemsBody.add(ItemService.gI().createItemNull());
            try {
                boolean cap2 = Util.isTrue(1, 2);
                short[] set;
                if (cap2) {
                    switch (pl.gender) {
                        case 0 -> set = new short[]{870, 871, 872};
                        case 1 -> set = new short[]{873, 874, 875};
                        default -> set = new short[]{867, 868, 869};
                    }
                } else {
                    if (pl.gender == 1) {
                        set = new short[]{391, 392, 393};
                    } else {
                        set = new short[]{383, 384, 385};
                    }
                }
                pl.botHead = set[0];
                pl.botBody = set[1];
                pl.botLeg = set[2];
            } catch (Exception ignored) {}
            try {
                int[] skillsArr = pl.gender == 0 ? new int[]{0, 1, 6, 9}
                        : pl.gender == 1 ? new int[]{2, 3, 7}
                        : new int[]{4, 5, 8};
                for (int sid : skillsArr) {
                    pl.playerSkill.skills.add(utils.SkillUtil.createSkill(sid, 1));
                }
                if (!pl.playerSkill.skills.isEmpty()) {
                    pl.playerSkill.skillSelect = pl.playerSkill.skills.get(0);
                }
            } catch (Exception ignored) {}
            map.Zone zoneJoin = null;
            for (int tries = 0; tries < 50 && zoneJoin == null; tries++) {
                map.Map m = Manager.MAPS.get(utils.Util.nextInt(Manager.MAPS.size()));
                if (isBannedMap(m.mapId)) continue;
                zoneJoin = services.MapService.gI().getMapWithRandZone(m.mapId);
            }
            if (zoneJoin == null) return;
            int triesXY2 = 0;
            int safeY2 = 100;
            do {
                pl.location.x = utils.Util.nextInt(100, Math.max(120, zoneJoin.map.mapWidth - 100));
                safeY2 = zoneJoin.map.yPhysicInTop(pl.location.x, 24);
                triesXY2++;
            } while (triesXY2 < 20 && safeY2 > zoneJoin.map.mapHeight / 2);
            if (safeY2 > zoneJoin.map.mapHeight / 2) {
                safeY2 = Math.min(safeY2, (zoneJoin.map.mapHeight / 2) - 10);
            }
            pl.location.y = Math.max(24, safeY2);
            services.func.ChangeMapService.gI().goToMap(pl, zoneJoin);
            zoneJoin.load_Me_To_Another(pl);
            this.put(pl);
            // Không start thread riêng cho bot, update trong Zone.update()
        } catch (Exception ignored) {}
    }

    public void clearBots() {
        for (int i = players.size() - 1; i >= 0; i--) {
            Player p = players.get(i);
            if (p != null && p.isBot) {
                remove(p);
            }
        }
    }

    public void clear() { clearBots(); }

    private String generateUniqueName(String[] n1, String[] n2, String[] n3){
        // Tạo tên ngẫu nhiên 4-6 ký tự [a-z0-9]
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        String name;
        int tries = 0;
        do {
            int len = utils.Util.nextInt(4, 6);
            StringBuilder sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                sb.append(alphabet.charAt(utils.Util.nextInt(alphabet.length())));
            }
            name = sb.toString();
            tries++;
        } while (players_name.containsKey(name) && tries < 50);
        return name;
    }

    private String ensureUniqueName(String base){
        if (base == null || base.isEmpty()) return generateUniqueName(null,null,null);
        String name = base;
        int tries = 0;
        while (players_name.containsKey(name) && tries < 50){
            name = base + utils.Util.nextInt(10,99);
            tries++;
        }
        return name;
    }

    private boolean isBannedMap(int mapId){
        int[] ban = {21,22,23,135,136,137,57,138,58,53,59,60,61,62,85,54,86,55,87,88,89,90,91,114,115,116,117,118,119,120,47,48,49,50,51,52,56};
        for(int id:ban){ if(id==mapId) return true; }
        return false;
    }

    private void remove(MySession session) {
        if (session.player != null) {
            this.remove(session.player);
            session.player.dispose();
        }
        if (session.joinedGame) {
            session.joinedGame = false;
            try {
                DBConnecter.executeUpdate("update account set last_time_logout = ? where id = ?", new Timestamp(System.currentTimeMillis()), session.userId);
            } catch (Exception e) {
                Logger.logException(Client.class, e);
            }
        }
        ServerManager.gI().disconnect(session);
    }

    private void remove(Player player) {
        this.players_id.remove(player.id);
        this.players_name.remove(player.name);
        if (player.getSession() != null) {
            this.players_userId.remove(player.getSession().userId);
        }
        this.players.remove(player);
        if (!player.beforeDispose) {
            player.beforeDispose = true;
            player.mapIdBeforeLogout = player.zone.map.mapId;
                TranhNgoc.gI().removePlayersBlue(player);
            TranhNgoc.gI().removePlayersRed(player);
            if (player.idNRNM != -1) {
                ItemMap itemMap = new ItemMap(player.zone, player.idNRNM, 1, player.location.x, player.location.y, -1);
                Service.gI().dropItemMap(player.zone, itemMap);
                NgocRongNamecService.gI().pNrNamec[player.idNRNM - 353] = "";
                NgocRongNamecService.gI().idpNrNamec[player.idNRNM - 353] = -1;
                player.idNRNM = -1;
            }
            ChangeMapService.gI().exitMap(player);
            TransactionService.gI().cancelTrade(player);
            if (player.clan != null) {
                player.clan.removeMemberOnline(null, player);
            }
            if (SummonDragon.gI().playerSummonShenron != null
                    && SummonDragon.gI().playerSummonShenron.id == player.id) {
                SummonDragon.gI().isPlayerDisconnect = true;
            }
            if (SummonDragonNamek.gI().playerSummonShenron != null
                    && SummonDragonNamek.gI().playerSummonShenron.id == player.id) {
                SummonDragonNamek.gI().isPlayerDisconnect = true;
            }
            if (player.shenronEvent != null) {
                player.shenronEvent.isPlayerDisconnect = true;
            }
            if (player.mobMe != null) {
                player.mobMe.mobMeDie();
            }
            if (player.pet != null) {
                if (player.pet.mobMe != null) {
                    player.pet.mobMe.mobMeDie();
                }
                ChangeMapService.gI().exitMap(player.pet);
            }
        }
        // Chỉ lưu player thật đã load đủ dữ liệu; bot/NPC hoặc player chưa init đầy đủ bỏ qua
        try {
            if (player != null && player.isPl() && player.iDMark != null && player.iDMark.isLoadedAllDataPlayer()) {
                PlayerDAO.updatePlayer(player);
            }
        } catch (Exception ignored) {}
    }

    public void kickSession(MySession session) {
        if (session != null) {
            session.disconnect();
            this.remove(session);
        }
    }

    public Player getPlayer(long playerId) {
        return this.players_id.get(playerId);
    }

    public Player getRandPlayer() {
        if (this.players.isEmpty()) {
            return null;
        }
        return this.players.get(Util.nextInt(players.size()));
    }

    public Player getPlayerByUser(int userId) {
        return this.players_userId.get(userId);
    }

    public Player getPlayer(String name) {
        return this.players_name.get(name);
    }

    public Player getPlayerByID(int playerId) {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player != null && player.id == playerId) {
                return player;
            }
        }
        return null;
    }

    public void close() {
        Logger.log(Logger.YELLOW, "BEGIN KICK OUT SESSION " + players.size() + "\n");
        while (!players.isEmpty()) {
            Player pl = players.remove(0);
            if (pl != null) {
                if (pl.getSession() != null) {
                    this.kickSession((MySession) pl.getSession());
                } else {
                    // bot hoặc player không có session
                    this.remove(pl);
                }
            }
        }
        Logger.success("SUCCESSFUL\n");
    }

    private void update() {
        for (int i = EmtiSessionManager.gI().getSessions().size() - 1; i >= 0; i--) {
            ISession s = EmtiSessionManager.gI().getSessions().get(i);
            MySession session = (MySession) s;
            if (session == null) {
                EmtiSessionManager.gI().getSessions().remove(i);
                continue;
            }
            if (session.timeWait > 0) {
                session.timeWait--;
                if (session.timeWait == 0) {
                    kickSession(session);
                }
            }
        }
    }

    @Override
    public void run() {
        while (ServerManager.isRunning) {
            long st = System.currentTimeMillis();
            try {
                update();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Functions.sleep(Math.max(1000 - (System.currentTimeMillis() - st), 10));
        }
    }

    public void show(Player player) {
        String txt = "";
        txt += "sessions: " + EmtiSessionManager.gI().getNumSession() + "\n";
        txt += "players_id: " + players_id.size() + "\n";
        txt += "players_userId: " + players_userId.size() + "\n";
        txt += "players_name: " + players_name.size() + "\n";
        txt += "players: " + players.size() + "\n";
        Service.gI().sendThongBao(player, txt);
    }
}
