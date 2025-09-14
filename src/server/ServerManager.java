package server;

/*
 *
 *
 * @author EMTI
 */
import EMTI.FileRunner;
import EMTI.Functions;
import boss.AnTromManager;
import boss.BrolyManager;
import minigame.DecisionMaker.DecisionMaker;
import minigame.LuckyNumber.LuckyNumber;
import models.Consign.ConsignShopManager;
import jdbc.daos.HistoryTransactionDAO;
import boss.BossManager;
import boss.OtherBossManager;
import boss.TreasureUnderSeaManager;
import boss.SnakeWayManager;
import boss.RedRibbonHQManager;
import boss.GasDestroyManager;
import boss.YardartManager;
import boss.ChristmasEventManager;
import boss.FinalBossManager;
import boss.HalloweenEventManager;
import boss.HungVuongEventManager;
import boss.LunarNewYearEventManager;
import boss.SkillSummonedManager;
import boss.TrungThuEventManager;
import consts.ConstDataEventNAP;
import consts.ConstDataEventSM;

import java.io.IOException;

import network.session.ISession;
import network.server.EMTIServer;
import server.io.MyKeyHandler;
import server.io.MySession;
import services.ClanService;
import services.NgocRongNamecService;
import utils.Logger;
import utils.TimeUtil;

import java.util.*;

import models.The23rdMartialArtCongress.The23rdMartialArtCongressManager;
import models.DeathOrAliveArena.DeathOrAliveArenaManager;
import event.EventManager;
import java.io.DataOutputStream;
import jdbc.daos.EventDAO;
import models.WorldMartialArtsTournament.WorldMartialArtsTournamentManager;
import network.example.MessageSendCollect;
import models.ShenronEvent.ShenronEventManager;
import models.SuperRank.SuperRankManager;
import network.io.Message;
import network.server.ISessionAcceptHandler;

public class ServerManager {

    public static String timeStart;

    public static final Map CLIENTS = new HashMap();

    public static String NAME = "Local";
    public static String IP = "127.0.0.1";
    public static int PORT = 1998;

    private static ServerManager instance;

    public static boolean isRunning;

    public void init() {
        Manager.gI();
        HistoryTransactionDAO.deleteHistory();
    }

    public static ServerManager gI() {
        if (instance == null) {
            instance = new ServerManager();
            instance.init();
        }
        return instance;
    }

    public static void main(String[] args) {
        timeStart = TimeUtil.getTimeNow("dd/MM/yyyy HH:mm:ss");
//        ServerManager.gI().run();
        new server.ServerManagerUI().setVisible(true);
    }

    public void run() {
        isRunning = true;
        activeServerSocket();
        activeCommandLine();
        utils.Threading.runLongLived(NgocRongNamecService.gI());
        utils.Threading.runLongLived(SuperRankManager.gI());
        utils.Threading.runLongLived(The23rdMartialArtCongressManager.gI());
        utils.Threading.runLongLived(DeathOrAliveArenaManager.gI());
        utils.Threading.runLongLived(WorldMartialArtsTournamentManager.gI());
        utils.Threading.runLongLived(AutoMaintenance.gI());
        utils.Threading.runLongLived(ShenronEventManager.gI());
//        new Thread(UpdateManager.gI(), "Update Manager").start();
//        new Thread(RemoteServerManager.gI(), "Remote Server Manager").start();
        BossManager.gI().loadBoss();
        Manager.MAPS.forEach(map.Map::initBoss);
        EventManager.gI().init();
        utils.Threading.runLongLived(BossManager.gI());
        utils.Threading.runLongLived(YardartManager.gI());
        utils.Threading.runLongLived(FinalBossManager.gI());
        utils.Threading.runLongLived(SkillSummonedManager.gI());
        utils.Threading.runLongLived(BrolyManager.gI());
        utils.Threading.runLongLived(AnTromManager.gI());
        utils.Threading.runLongLived(OtherBossManager.gI());
        utils.Threading.runLongLived(RedRibbonHQManager.gI());
        utils.Threading.runLongLived(TreasureUnderSeaManager.gI());
        utils.Threading.runLongLived(SnakeWayManager.gI());
        utils.Threading.runLongLived(GasDestroyManager.gI());
        utils.Threading.runLongLived(TrungThuEventManager.gI());
        utils.Threading.runLongLived(HalloweenEventManager.gI());
        utils.Threading.runLongLived(ChristmasEventManager.gI());
        utils.Threading.runLongLived(HungVuongEventManager.gI());
        utils.Threading.runLongLived(LunarNewYearEventManager.gI());
        utils.Threading.runLongLived(LuckyNumber.gI());
        utils.Threading.runLongLived(DecisionMaker.gI());
        // Khởi tạo WeeklyRewardService
        services.WeeklyRewardService.gI();
        // Chuyển sang scheduler 1s để giảm CPU và tránh vòng lặp bận
        utils.Threading.scheduler().scheduleAtFixedRate(() -> {
            try {
                ConstDataEventSM.isRunningSK = ConstDataEventSM.isActiveEvent();
                ConstDataEventNAP.isRunningSK = ConstDataEventNAP.isActiveEvent();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);

    }

    private void activeServerSocket() {
        try {
            EMTIServer.gI().init().setAcceptHandler(new ISessionAcceptHandler() {
                @Override
                public void sessionInit(ISession is) {
                    if (!canConnectWithIp(is.getIP())) {
                        is.disconnect();
                        return;
                    }
                    is.setMessageHandler(Controller.gI())
                            .setSendCollect(new MessageSendCollect(){
                                @Override
                                public void doSendMessage(ISession session, DataOutputStream dos, Message msg) throws Exception {
                                    try {
                                        byte[] data = msg.getData();
                                        if (session.sentKey()) {
                                            byte b = this.writeKey(session, msg.command);
                                            dos.writeByte(b);
                                        } else {
                                            dos.writeByte(msg.command);
                                        }
                                        if (data != null) {
                                            int size = data.length;
                                            if (msg.command == -32 || msg.command == -66 || msg.command == -74 || msg.command == 11 || msg.command == -67 || msg.command == -87 || msg.command == 66) {
                                                byte b2 = this.writeKey(session, (byte) size);
                                                dos.writeByte(b2 - 128);
                                                byte b3 = this.writeKey(session, (byte) (size >> 8));
                                                dos.writeByte(b3 - 128);
                                                byte b4 = this.writeKey(session, (byte) (size >> 16));
                                                dos.writeByte(b4 - 128);
                                            } else if (session.sentKey()) {
                                                byte byte1 = this.writeKey(session, (byte) (size >> 8));
                                                dos.writeByte(byte1);
                                                byte byte2 = this.writeKey(session, (byte) (size & 0xFF));
                                                dos.writeByte(byte2);
                                            } else {
                                                dos.writeShort(size);
                                            }
                                            if (session.sentKey()) {
                                                for (int i = 0; i < data.length; ++i) {
                                                    data[i] = this.writeKey(session, data[i]);
                                                }
                                            }
                                            dos.write(data);
                                        } else {
                                            dos.writeShort(0);
                                        }
                                        dos.flush();
                                        msg.cleanup();
                                    } catch (IOException iOException) {
                                        // empty catch block
                                    }
                                }
                            })
                            .setKeyHandler(new MyKeyHandler())
                            .startCollect();
                }

                @Override
                public void sessionDisconnect(ISession session) {
                    Client.gI().kickSession((MySession) session);
                }
            }).setTypeSessioClone(MySession.class)
                    .setDoSomeThingWhenClose(() -> {
                        Logger.error("SERVER CLOSE\n");
                        System.exit(0);
                    })
                    .start(PORT);
        } catch (Exception e) {
        }
    }

    private boolean canConnectWithIp(String ipAddress) {
        Object o = CLIENTS.get(ipAddress);
        if (o == null) {
            CLIENTS.put(ipAddress, 1);
            return true;
        } else {
            int n = Integer.parseInt(String.valueOf(o));
            if (n < Manager.MAX_PER_IP) {
                n++;
                CLIENTS.put(ipAddress, n);
                return true;
            } else {
                return false;
            }
        }
    }

    private void activeCommandLine() {
        new Thread(() -> {
            Scanner sc = new Scanner(System.in);
            while (true) {
                String line = sc.nextLine();
                if (line.equals("baotri")) {
                    new Thread(() -> {
                        Maintenance.gI().start(5);
                    }).start();
                } else if (line.equals("athread")) {
                    System.out.println("Số thread hiện tại của Server ULTRAKILL: " + Thread.activeCount());
                } else if (line.equals("nplayer")) {
                    System.out.println("Số lượng người chơi hiện tại của Server ULTRAKILL: " + Client.gI().getPlayers().size());
                } else if (line.equals("shop")) {
                    Manager.gI().updateShop();
                    System.out.println("===========================DONE UPDATE SHOP===========================");
                } else if (line.equals("a")) {
                    new Thread(() -> {
                        Client.gI().close();
                    }).start();
                }
            }
        }, "Active line").start();
    }

    public void disconnect(MySession session) {
        Object o = CLIENTS.get(session.getIP());
        if (o != null) {
            int n = Integer.parseInt(String.valueOf(o));
            n--;
            if (n < 0) {
                n = 0;
            }
            CLIENTS.put(session.getIP(), n);
        }
    }

    public void close() {
        isRunning = false;
        try {
            ClanService.gI().close();
        } catch (Exception e) {
            Logger.error("Lỗi save clan!\n");
        }
        try {
            ConsignShopManager.gI().save();
        } catch (Exception e) {
            Logger.error("Lỗi save shop ký gửi!\n");
        }
        Client.gI().close();
        EventDAO.save();
        Logger.success("SUCCESSFULLY MAINTENANCE!\n");

//        if (AutoMaintenance.isRunning) {
//            AutoMaintenance.isRunning = false;
        try {
            String batchFilePath = "run.bat";
            FileRunner.runBatchFile(batchFilePath);
        } catch (IOException e) {
        }
//        }
        System.exit(0);
    }
}
