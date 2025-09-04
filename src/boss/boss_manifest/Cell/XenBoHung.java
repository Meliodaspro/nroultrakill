package boss.boss_manifest.Cell;

/*
 *
 *
 * @author EMTI
 */

import consts.ConstPlayer;
import boss.Boss;
import boss.BossesData;
import boss.BossID;
import map.ItemMap;
import player.Player;
import services.EffectSkillService;
import services.PlayerService;
import services.Service;
import services.TaskService;
import utils.Util;
import services.func.ChangeMapService;

public class XenBoHung extends Boss {

    private long lastTimeHapThu;
    private int timeHapThu;

    public XenBoHung() throws Exception {
        super(BossID.XEN_BO_HUNG, BossesData.XEN_BO_HUNG_1, BossesData.XEN_BO_HUNG_2, BossesData.XEN_BO_HUNG_3);
    }

    @Override
    public void reward(Player plKill) {
        // Rơi item ID 16, số lượng 1 (luôn rơi)
        ItemMap it = new ItemMap(this.zone, 16, 1, this.location.x, this.location.y, plKill.id);
        Service.gI().dropItemMap(this.zone, it);

        // Kiểm tra nhiệm vụ khi giết boss
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);

        // 5% rơi item 1229 số lượng 25-50
        if (Util.isTrue(5, 50)) {
            for (int i = 0; i < Util.nextInt(25, 50); i++) {
                ItemMap it2 = new ItemMap(this.zone, 1229, 1,
                        this.location.x + Util.nextInt(-15, 15),
                        this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24),
                        plKill.id);
                Service.gI().dropItemMap(this.zone, it2);
            }
        }

        // 30% rơi item ID 15, số lượng 1
        if (Util.isTrue(30, 100)) {
            ItemMap item15 = new ItemMap(this.zone, 15, 1,
                    this.location.x + Util.nextInt(-15, 15),
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24),
                    plKill.id);
            Service.gI().dropItemMap(this.zone, item15);
        }

        // 20% rơi item ID 457, số lượng 10-15
        if (Util.isTrue(20, 100)) {
            int quantity = Util.nextInt(10, 15);
            ItemMap item457 = new ItemMap(this.zone, 457, quantity,
                    this.location.x + Util.nextInt(-15, 15),
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24),
                    plKill.id);
            Service.gI().dropItemMap(this.zone, item457);
        }
//        sự kiện
        int quantity = Util.nextInt(1, 3);
        ItemMap item1743 = new ItemMap(this.zone, 1743, quantity, this.location.x,
                this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
        Service.gI().dropItemMap(this.zone, item1743);
    }


    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.hapThu();
        this.attack();
    }

    private void hapThu() {
        if (!Util.canDoWithTime(this.lastTimeHapThu, this.timeHapThu) || !Util.isTrue(1, 100)) {
            return;
        }

        Player pl = this.zone.getRandomPlayerInMap();
        if (pl == null || pl.isDie()) {
            return;
        }
        ChangeMapService.gI().changeMapYardrat(this, this.zone, pl.location.x, pl.location.y);
        this.nPoint.dameg += (pl.nPoint.dame * 5 / 100);
        this.nPoint.hpg += (pl.nPoint.hp * 2 / 100);
        this.nPoint.critg++;
        this.nPoint.calPoint();
        PlayerService.gI().hoiPhuc(this, pl.nPoint.hp, 0);
        pl.injured(null, pl.nPoint.hpMax, true, false);
        Service.gI().sendThongBao(pl, "Bạn vừa bị " + this.name + " hấp thu!");
        this.chat(2, "Ui cha cha, kinh dị quá. " + pl.name + " vừa bị tên " + this.name + " nuốt chửng kìa!!!");
        this.chat("Haha, ngọt lắm đấy " + pl.name + "..");
        this.lastTimeHapThu = System.currentTimeMillis();
        this.timeHapThu = Util.nextInt(10000, 20000);
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage / 2);
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage / 4;
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return damage;
        } else {
            return 0;
        }
    }

}
