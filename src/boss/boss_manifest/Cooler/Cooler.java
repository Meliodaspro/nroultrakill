package boss.boss_manifest.Cooler;

/*
 *
 *
 * @author EMTI
 */
import boss.Boss;
import boss.BossID;
import boss.BossesData;
import consts.ConstTaskBadges;
import item.Item;
import map.ItemMap;
import player.Player;
import services.EffectSkillService;
import services.Service;
import utils.Util;

import java.util.Random;
import services.TaskService;
import task.Badges.BadgesTaskService;

public class Cooler extends Boss {

    private long st;

    public Cooler() throws Exception {
        super(BossID.COOLER, BossesData.COOLER, BossesData.COOLER_2);
    }

    @Override
    public void reward(Player plKill) {
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        BadgesTaskService.updateCountBagesTask(plKill, ConstTaskBadges.TRUM_SAN_BOSS, 1);
        // Danh sách item từ 555 đến 567
        int[] itemSpecial = new int[]{555, 556, 557, 558, 559, 560, 561, 562, 563, 564, 565, 566, 567};
        int randomSpecial = new Random().nextInt(itemSpecial.length);

        // Luôn rơi item 15 với số lượng 1
        ItemMap item15 = new ItemMap(this.zone, 15, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
        Service.gI().dropItemMap(this.zone, item15);

        // 20% cơ hội rơi item đặc biệt (555 - 567)
        if (Util.isTrue(20, 100)) {
            ItemMap specialItem = new ItemMap(this.zone, itemSpecial[randomSpecial], 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, specialItem);
        }
        if (Util.isTrue(20, 100)) {
            ItemMap item674 = new ItemMap(this.zone, 674, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
            Service.gI().dropItemMap(this.zone, item674);
        }
        //        sự kiện
        int quantity = Util.nextInt(4,6);
        ItemMap item1743 = new ItemMap(this.zone, 1743, quantity, this.location.x,
                this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), plKill.id);
        Service.gI().dropItemMap(this.zone, item1743);
    }
    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (piercing) {
                damage /= 100;
            }
            if (Util.isTrue(200, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            if (effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }
            if (damage > 10_000_000) {
                damage = Util.nextInt(9_000_000, 10_000_000);
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

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

}
