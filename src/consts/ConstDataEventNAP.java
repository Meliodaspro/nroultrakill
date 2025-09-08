package consts;

import EMTI.Functions;
import item.Item;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.simple.JSONObject;
import services.ItemService;
import services.Service;
import jdbc.DBConnecter;

import jdbc.daos.NDVSqlFetcher;
import java.util.ArrayList;
import java.util.Calendar;

import java.util.List;
import jdbc.NDVResultSet;
import player.Player;
import utils.Logger;
import java.io.*;
import java.util.Properties;

// su kien 1/6
public class ConstDataEventNAP {//Zalo: 0358124452//Name: EMTI 

    public static ConstDataEventNAP gI;

    public static ConstDataEventNAP gI() {//Zalo: 0358124452//Name: EMTI 
        if (gI == null) {//Zalo: 0358124452//Name: EMTI 
            gI = new ConstDataEventNAP();
        }
        return gI;
    }

    public static boolean isEventActive() {//Zalo: 0358124452//Name: EMTI 
        return false;
    }

    public static boolean isTraoQua = true;

    public static Calendar startEvent;

    public static Calendar endEvents;

    public static boolean initsukien = false;

    public static byte MONTH_OPEN = 00;
    public static byte DATE_OPEN = 00;
    public static byte HOUR_OPEN = 00;
    public static byte MIN_OPEN = 00;

    public static byte MONTH_END = 00;
    public static byte DATE_END = 00;
    public static byte HOUR_END = 00;
    public static byte MIN_END = 00;
    
    // Static block để load config ngay khi class được load
    static {
        loadEventTimeFromFile();
    }

    public static boolean isActiveEvent() {
        // Kiểm tra nếu thời gian = 0 thì không có sự kiện
        if (MONTH_OPEN == 0 || DATE_OPEN == 0 || MONTH_END == 0 || DATE_END == 0) {
            if (!initsukien) {
                System.out.println("Sự kiện nạp thẻ đã bị tắt (thời gian = 0)");
                initsukien = true;
            }
            return false;
        }
        
        // Kiểm tra thời gian có hợp lệ không (bắt đầu trong quá khứ, kết thúc trong tương lai)
        Calendar now = Calendar.getInstance();
        Calendar endCheck = Calendar.getInstance();
        endCheck.set(2025, MONTH_END - 1, DATE_END, HOUR_END, MIN_END);
        
        if (endCheck.getTimeInMillis() < now.getTimeInMillis()) {
            if (!initsukien) {
                System.out.println("Sự kiện nạp thẻ đã kết thúc (thời gian kết thúc trong quá khứ: " + 
                    DATE_END + "/" + MONTH_END + "/2025 " + HOUR_END + ":" + String.format("%02d", MIN_END) + ")");
                initsukien = true;
            }
            return false;
        }
        
        if (!initsukien) {
            initsukien = true;
            startEvent = Calendar.getInstance();

            // Thiết lập ngày và giờ bắt đầu
            startEvent.set(2025, MONTH_OPEN - 1, DATE_OPEN, HOUR_OPEN, MIN_OPEN);
            System.out.println("Ngày bắt đầu sự kiện đua top nạp: " + startEvent.getTime());

            endEvents = Calendar.getInstance();
            // Thiết lập ngày và giờ kết thúc
            endEvents.set(2025, MONTH_END - 1, DATE_END, HOUR_END, MIN_END);
            System.out.println("Ngày kết thúc sự kiện đua top nap: " + endEvents.getTime());
        }

        Calendar currentTime = Calendar.getInstance();
        if (System.currentTimeMillis() >= startEvent.getTimeInMillis() && System.currentTimeMillis() <= endEvents.getTimeInMillis()) {
            if (isTraoQua && System.currentTimeMillis() + 60000 >= endEvents.getTimeInMillis()) {
                String sql = "SELECT  player.id as plid, player.name as name, account.danap FROM account, player WHERE account.id = player.account_id AND account.create_time > '2025-" + MONTH_OPEN + "-" + DATE_OPEN + " " + HOUR_OPEN + ":" + MIN_OPEN + ":00' AND account.danap >= 100000 ORDER BY account.danap DESC LIMIT 10;";
                List<Integer> AccIdPlayer = new ArrayList<>();
                NDVResultSet rs = null;
                try {
                    rs = DBConnecter.executeQuery(sql);
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        AccIdPlayer.add(id);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for (int i = 0; i < AccIdPlayer.size(); i++) {
                    Player player = NDVSqlFetcher.loadPlayerByID(AccIdPlayer.get(i));
                    TraoQuaSuKien(player, i + 1);
                    Logger.error("Đã trao quà nạp top " + (i + 1) + " cho: " + player.name + "\n");
                    try {
                        //Thread.sleep(100);
                        Functions.sleep(100);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                isTraoQua = false;
            }
            return true;
        } else {

            return false;
        }
    }

    public static boolean isRunningSK = isActiveEvent();

    public static void TraoQuaSuKien(Player pl, int rank) {
        Item item = null;
        JSONArray dataArray;
        JSONObject dataObject;
        try ( Connection con2 = DBConnecter.getConnectionServer();  PreparedStatement ps = con2.prepareStatement("SELECT detail FROM moc_nap_top WHERE id = ?")) {
            // Sử dụng rank (thứ hạng) để truy vấn phần thưởng tương ứng
            ps.setInt(1, rank);
            try ( ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    dataArray = (JSONArray) JSONValue.parse(rs.getString("detail"));
                    for (int i = 0; i < dataArray.size(); i++) {
                        dataObject = (JSONObject) JSONValue.parse(String.valueOf(dataArray.get(i)));
                        int tempid = Integer.parseInt(String.valueOf(dataObject.get("temp_id")));
                        int quantity = Integer.parseInt(String.valueOf(dataObject.get("quantity")));
                        item = ItemService.gI().createNewItem((short) tempid);
                        item.quantity = quantity;
                        JSONArray optionsArray = (JSONArray) dataObject.get("options");
                        for (int j = 0; j < optionsArray.size(); j++) {
                            JSONObject optionObject = (JSONObject) optionsArray.get(j);
                            int param = Integer.parseInt(String.valueOf(optionObject.get("param")));
                            int optionId = Integer.parseInt(String.valueOf(optionObject.get("id")));
                            item.itemOptions.add(new Item.ItemOption(optionId, param));
                        }
                        pl.inventory.itemsMailBox.add(item);
                    }
                    if (NDVSqlFetcher.updateMailBox(pl)) {
                        Service.gI().sendThongBao(pl, "Bạn vừa nhận quà về mail thành công");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Method để cập nhật thời gian sự kiện từ admin panel
    public static void updateEventTime(int startMonth, int startDay, int startHour, int startMin,
                                     int endMonth, int endDay, int endHour, int endMin) {
        // Cập nhật các field static để sự kiện chạy với thời gian mới
        MONTH_OPEN = (byte) startMonth;
        DATE_OPEN = (byte) startDay;
        HOUR_OPEN = (byte) startHour;
        MIN_OPEN = (byte) startMin;
        
        MONTH_END = (byte) endMonth;
        DATE_END = (byte) endDay;
        HOUR_END = (byte) endHour;
        MIN_END = (byte) endMin;
        
        // Reset flag để khởi tạo lại thời gian sự kiện
        initsukien = false;
        
        System.out.println("Đã cập nhật thời gian sự kiện nạp thẻ:");
        System.out.println("Bắt đầu: " + startDay + "/" + startMonth + "/2025 " + startHour + ":" + String.format("%02d", startMin));
        System.out.println("Kết thúc: " + endDay + "/" + endMonth + "/2025 " + endHour + ":" + String.format("%02d", endMin));
        
        // Lưu thời gian vào file để nhớ sau restart
        saveEventTimeToFile();
    }
    
    // Method để load thời gian từ file config.properties
    private static void loadEventTimeFromFile() {
        File configFile = new File("data/config/config.properties");
        if (!configFile.exists()) {
            System.out.println("File data/config/config.properties không tồn tại, sử dụng thời gian mặc định");
            return;
        }
        
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);
            
            // Load thời gian bắt đầu
            String startMonthStr = props.getProperty("nap_event.start_month", "00");
            String startDayStr = props.getProperty("nap_event.start_day", "00");
            String startHourStr = props.getProperty("nap_event.start_hour", "00");
            String startMinStr = props.getProperty("nap_event.start_min", "00");
            
            // Load thời gian kết thúc
            String endMonthStr = props.getProperty("nap_event.end_month", "00");
            String endDayStr = props.getProperty("nap_event.end_day", "00");
            String endHourStr = props.getProperty("nap_event.end_hour", "00");
            String endMinStr = props.getProperty("nap_event.end_min", "00");
            
            // Cập nhật các giá trị static
            MONTH_OPEN = Byte.parseByte(startMonthStr);
            DATE_OPEN = Byte.parseByte(startDayStr);
            HOUR_OPEN = Byte.parseByte(startHourStr);
            MIN_OPEN = Byte.parseByte(startMinStr);
            
            MONTH_END = Byte.parseByte(endMonthStr);
            DATE_END = Byte.parseByte(endDayStr);
            HOUR_END = Byte.parseByte(endHourStr);
            MIN_END = Byte.parseByte(endMinStr);
            
            // Reset flag để sự kiện sử dụng thời gian mới
            initsukien = false;
            
            System.out.println("Đã load thời gian sự kiện nạp thẻ từ file:");
            System.out.println("Bắt đầu: " + DATE_OPEN + "/" + MONTH_OPEN + "/2025 " + HOUR_OPEN + ":" + String.format("%02d", MIN_OPEN));
            System.out.println("Kết thúc: " + DATE_END + "/" + MONTH_END + "/2025 " + HOUR_END + ":" + String.format("%02d", MIN_END));
            
        } catch (IOException | NumberFormatException e) {
            System.out.println("Lỗi khi load thời gian từ file config.properties: " + e.getMessage());
            System.out.println("Sử dụng thời gian mặc định");
        }
    }
    
    // Method để save thời gian vào file config.properties
    private static void saveEventTimeToFile() {
        File configFile = new File("data/config/config.properties");
        
        try {
            // Đọc toàn bộ file hiện tại
            StringBuilder content = new StringBuilder();
            if (configFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                }
            }
            
            String configContent = content.toString();
            
            // Cập nhật các giá trị thời gian sự kiện nạp thẻ
            System.out.println("Đang cập nhật config với giá trị:");
            System.out.println("MONTH_OPEN=" + MONTH_OPEN + ", DATE_OPEN=" + DATE_OPEN + ", HOUR_OPEN=" + HOUR_OPEN + ", MIN_OPEN=" + MIN_OPEN);
            System.out.println("MONTH_END=" + MONTH_END + ", DATE_END=" + DATE_END + ", HOUR_END=" + HOUR_END + ", MIN_END=" + MIN_END);
            
            configContent = updateConfigValue(configContent, "nap_event.start_month", String.valueOf(MONTH_OPEN));
            configContent = updateConfigValue(configContent, "nap_event.start_day", String.valueOf(DATE_OPEN));
            configContent = updateConfigValue(configContent, "nap_event.start_hour", String.valueOf(HOUR_OPEN));
            configContent = updateConfigValue(configContent, "nap_event.start_min", String.valueOf(MIN_OPEN));
            configContent = updateConfigValue(configContent, "nap_event.end_month", String.valueOf(MONTH_END));
            configContent = updateConfigValue(configContent, "nap_event.end_day", String.valueOf(DATE_END));
            configContent = updateConfigValue(configContent, "nap_event.end_hour", String.valueOf(HOUR_END));
            configContent = updateConfigValue(configContent, "nap_event.end_min", String.valueOf(MIN_END));
            
            // Lưu file với format đẹp
            try (FileWriter writer = new FileWriter(configFile)) {
                writer.write(configContent);
            }
            
            System.out.println("Đã lưu thời gian sự kiện nạp thẻ vào file config.properties");
            
        } catch (IOException e) {
            System.out.println("Lỗi khi lưu file config.properties: " + e.getMessage());
        }
    }
    
    // Helper method để cập nhật giá trị trong config mà giữ nguyên format
    private static String updateConfigValue(String content, String key, String value) {
        // Tách thành các dòng
        String[] lines = content.split("\n");
        StringBuilder result = new StringBuilder();
        boolean found = false;
        
        for (String line : lines) {
            if (line.trim().startsWith(key + "=")) {
                // Tìm thấy key, cập nhật giá trị
                result.append(key).append("=").append(value).append("\n");
                found = true;
                System.out.println("Đã cập nhật key '" + key + "' = " + value);
            } else {
                // Giữ nguyên dòng khác
                result.append(line).append("\n");
            }
        }
        
        // Nếu không tìm thấy key, thêm vào cuối
        if (!found) {
            result.append(key).append("=").append(value).append("\n");
            System.out.println("Không tìm thấy key '" + key + "' trong config, thêm vào cuối file");
        }
        
        return result.toString();
    }
    
    // Method để force reload thời gian từ config (dùng khi cần sync)
    public static void reloadEventTimeFromConfig() {
        loadEventTimeFromFile();
        // Reset flag để sự kiện sử dụng thời gian mới
        initsukien = false;
    }
}
