package event;

/**
 *
 * @author EMTI
 */

import event.event_manifest.TopUp;
import event.event_manifest.TrungThu;
import event.event_manifest.HungVuong;
import event.event_manifest.Christmas;
import event.event_manifest.Default;
import event.event_manifest.Halloween;
import event.event_manifest.LunarNewYear;
//import event.event_manifest.Default;
import event.event_manifest.InternationalWomensDay;

public class EventManager {

    private static EventManager instance;

    public static boolean LUNNAR_NEW_YEAR = false;

    public static boolean INTERNATIONAL_WOMANS_DAY = false;

    public static boolean CHRISTMAS = false;

    public static boolean HALLOWEEN = false;

    public static boolean HUNG_VUONG = false;

    public static boolean TRUNG_THU = false;

    public static boolean TOP_UP = false;

    public static EventManager gI() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    public void init() {
        // Luôn khởi tạo Default event
        new Default().init();
        
        // Khởi tạo các sự kiện theo mùa
        if (LUNNAR_NEW_YEAR) {
            new LunarNewYear().init();
        }
        if (INTERNATIONAL_WOMANS_DAY) {
            new InternationalWomensDay().init();
        }
        if (HALLOWEEN) {
            new Halloween().init();
        }
        if (CHRISTMAS) {
            new Christmas().init();
        }
        if (HUNG_VUONG) {
            new HungVuong().init();
        }
        if (TRUNG_THU) {
            new TrungThu().init();
        }
        if (TOP_UP) {
            new TopUp().init();
        }
    }

    // Method để khởi tạo lại sự kiện (dùng cho admin panel)
    public void reinit() {
        System.out.println("Đang khởi tạo lại sự kiện...");
        init();
        System.out.println("Khởi tạo lại sự kiện hoàn tất!");
    }
}
