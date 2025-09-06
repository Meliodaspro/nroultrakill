package server;

//import EmtiManager.EmtiManager;
import consts.cn;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.prefs.Preferences;
import jdbc.daos.EventDAO;
import models.Consign.ConsignShopManager;
import network.SessionManager;
import network.server.EmtiSessionManager;

import services.ClanService;
import utils.Logger;

public class ServerManagerUI extends JFrame {

    private Preferences preferences;
    private JLabel ssCountLabel;
    private JLabel plCountLabel;
    private JLabel threadCountLabel;
    private JLabel uptimeLabel;
    private JTextField minutesField;
    private JLabel messageLabel;
    private JLabel countdownLabel;
    private Timer countdownTimer;
    private int remainingSeconds;
    private ButtonGroup maintenanceGroup;
// Thêm checkbox
    private JCheckBox maintenanceOption1;
    private JCheckBox maintenanceOption2;
    private JLabel info;
    private long serverStartTime;

    public ServerManagerUI() {
        preferences = Preferences.userNodeForPackage(ServerManagerUI.class);
        serverStartTime = System.currentTimeMillis();
        setTitle("Chương trình Bảo trì ULTRAKILL SV" + cn.SV + "");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });
        JPanel panel = new JPanel();
        getContentPane().add(panel);

//        JButton startButton = new JButton("Start Server");
//        startButton.addActionListener(e -> startServer());
//        panel.add(startButton);
        JButton maintenanceButton = new JButton("Bảo trì");
        maintenanceButton.addActionListener(e -> showMaintenanceDialog());
        panel.add(maintenanceButton);

        JButton eventButton = new JButton("Quản lý Sự kiện");
        eventButton.addActionListener(e -> showEventManagerDialog());
        panel.add(eventButton);
        
        // Nút quản lý thời gian sự kiện nạp thẻ
        JButton timeEventButton = new JButton("Thời gian Sự kiện Nạp thẻ");
        timeEventButton.addActionListener(e -> showTimeEventDialog());
        panel.add(timeEventButton);
//        JLabel jLabel = new JLabel("Cài đặt số phút bảo trì");
//        panel.add(jLabel);
//        minutesField = new JTextField(5);
//        panel.add(minutesField);
//
//        JButton scheduleButton = new JButton("Hẹn phút bảo trì");
//        scheduleButton.addActionListener(e -> scheduleMaintenance());
//        panel.add(scheduleButton);
        JLabel jLabel2 = new JLabel("Cài đặt giờ bảo trì");
        panel.add(jLabel2);
        info = new JLabel("");
        // Đọc giá trị từ tệp tin
        try ( BufferedReader reader = new BufferedReader(new FileReader("maintenanceConfig.txt"))) {
            String hoursLine = reader.readLine();
            String minutesLine = reader.readLine();
            String secondsLine = reader.readLine();

            int hours = Integer.parseInt(hoursLine);
            int minutes = Integer.parseInt(minutesLine);
            int seconds = Integer.parseInt(secondsLine);

            // Thêm giá trị vào DefaultComboBoxModel
            DefaultComboBoxModel<Integer> hoursModel = new DefaultComboBoxModel<>();
            for (int i = -1; i < 24; i++) {
                hoursModel.addElement(i);
            }
            JComboBox<Integer> hoursComboBox = new JComboBox<>(hoursModel);
            panel.add(hoursComboBox);
            hoursComboBox.setSelectedItem(hours);

            // Thêm giá trị vào DefaultComboBoxModel
            DefaultComboBoxModel<Integer> minutesModel = new DefaultComboBoxModel<>();
            for (int i = -1; i < 60; i++) {
                minutesModel.addElement(i);
            }
            JComboBox<Integer> minutesComboBox = new JComboBox<>(minutesModel);
            panel.add(minutesComboBox);
            minutesComboBox.setSelectedItem(minutes);

            // Thêm giá trị vào DefaultComboBoxModel
            DefaultComboBoxModel<Integer> secondsModel = new DefaultComboBoxModel<>();
            for (int i = -1; i < 60; i++) {
                secondsModel.addElement(i);
            }
            JComboBox<Integer> secondsComboBox = new JComboBox<>(secondsModel);
            panel.add(secondsComboBox);
            secondsComboBox.setSelectedItem(seconds);
            JButton scheduleButton2 = new JButton("Hẹn giờ bảo trì");
            scheduleButton2.addActionListener(e -> scheduleMaintenance(hoursComboBox, minutesComboBox, secondsComboBox));
            panel.add(scheduleButton2);
            if (hours != -1 && minutes != -1 && seconds != -1) {
                scheduleMaintenance(hoursComboBox, minutesComboBox, secondsComboBox);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        JButton saveButton = new JButton("Lưu Data");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Logger.success("Đang tiến hành lưu data");
                network.server.EMTIServer.gI().stopConnect();

                Maintenance.isRunning = false;
                try {
                    Logger.error("Đang tiến hành lưu data bang hội");
                    ClanService.gI().close();
                    Thread.sleep(1000);
                    Logger.success("Lưu dữ liệu bang hội thành công");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Logger.error("Lỗi lưu dữ liệu bang hội");
                }
                try {
                    Logger.error("Đang tiến hành lưu data ký gửi");
                    ConsignShopManager.gI().save();
                    Thread.sleep(1000);
                    Logger.success("Lưu dữ liệu ký gửi thành công");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Logger.error("Lỗi lưu dữ liệu ký gửi");
                }

                try {
                    Logger.error("Đang tiến hành đẩy người chơi");
                    Client.gI().close();
                    EventDAO.save();
                    Thread.sleep(1000);
                    Logger.success("Lưu dữ liệu người dùng thành công");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Logger.error("Lỗi lưu dữ liệu người dùng");
                }
                System.exit(0);
            }
        });
        panel.add(saveButton);

//        
        JButton clearFw = new JButton("clearFw");
        clearFw.addActionListener((ActionEvent e) -> {
            try {
                // Lấy số lượng IP bị khóa trước khi xóa
                int count = server.io.MySession.getAntiLoginCount();
                
                // Xóa tất cả dữ liệu AntiLogin
                server.io.MySession.clearAntiLogin();
                
                // Hiển thị thông báo
                JOptionPane.showMessageDialog(null, 
                    "Đã xóa " + count + " IP bị khóa trong AntiLogin\n" +
                    "Tất cả IP có thể đăng nhập lại bình thường");
                
                Logger.success("Admin đã xóa " + count + " IP bị khóa AntiLogin");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "Lỗi khi xóa AntiLogin: " + ex.getMessage());
                Logger.error("Lỗi xóa AntiLogin: " + ex.getMessage());
            }
        });
        panel.add(clearFw);
        // Thêm checkbox vào JPanel
        messageLabel = new JLabel();
        panel.add(messageLabel);

        countdownLabel = new JLabel();
        panel.add(countdownLabel);

        panel.add(info);
        uptimeLabel = new JLabel("Uptime: 00:00:00");
        panel.add(uptimeLabel);
        threadCountLabel = new JLabel("Số Thread : ");
        panel.add(threadCountLabel);
        plCountLabel = new JLabel("Online :");
        panel.add(plCountLabel);
        ssCountLabel = new JLabel("Session :");
        panel.add(ssCountLabel);

        ScheduledExecutorService threadCountExecutor = Executors.newSingleThreadScheduledExecutor();
        threadCountExecutor.scheduleAtFixedRate(() -> {
            int threadCount = Thread.activeCount();
            threadCountLabel.setText("Số thread: " + threadCount);
        }, 1, 1, TimeUnit.SECONDS);

        ScheduledExecutorService plCountExecutor = Executors.newSingleThreadScheduledExecutor();
        plCountExecutor.scheduleAtFixedRate(() -> {
            int plcount = Client.gI().getPlayers().size();
            plCountLabel.setText("Online : " + plcount);
        }, 5, 1, TimeUnit.SECONDS);

        ScheduledExecutorService ssCountExecutor = Executors.newSingleThreadScheduledExecutor();
        ssCountExecutor.scheduleAtFixedRate(() -> {
            int sscount = SessionManager.gI().getSessions().size();
            ssCountLabel.setText("Session : " + sscount);
        }, 5, 1, TimeUnit.SECONDS);

        ScheduledExecutorService uptimeExecutor = Executors.newSingleThreadScheduledExecutor();
        uptimeExecutor.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            long uptimeMillis = currentTime - serverStartTime;
            String uptimeStr = formatUptime(uptimeMillis);
            uptimeLabel.setText("Uptime: " + uptimeStr);
        }, 0, 1, TimeUnit.SECONDS);

        setVisible(true);
        messageLabel.setText("Server đang chạy");

        ServerManager.gI().run();
//        EmtiManager.getInstance().startAutoSave();
        // Đọc giá trị từ tệp
    }

    public void close(long delay) {
        network.server.EMTIServer.gI().stopConnect();

        Maintenance.isRunning = false;
        ServerManager.gI().close();
        System.exit(0);
        Logger.error("BEGIN MAINTENANCE...............................\n");

    }

    private void showMaintenanceDialog() {
        try {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(this, "Bắt đầu bảo trì?", "Bảo trì", dialogButton);
            if (dialogResult == 0) {
                Logger.error("Server tiến hành bảo trì");
                Maintenance.gI().start(15);

            } else {
                System.out.println("No Option");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void scheduleMaintenance() {
        String minutesStr = minutesField.getText();
        try {
            int minutes = Integer.parseInt(minutesStr);
            if (minutes <= 0) {
                messageLabel.setText("Số phút phải lớn hơn 0");
                return;
            }
            // Lưu giá trị vào tệp
            try {
                File file = new File("maintenanceTime.txt");
                FileWriter fw = new FileWriter(file);
                fw.write(String.valueOf(minutes));
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            long delay = minutes * 60L * 1000L;
            remainingSeconds = minutes * 60;
            countdownLabel.setText("Thời gian còn lại: " + formatTime(remainingSeconds));
            countdownTimer = new Timer(1000, e -> {
                remainingSeconds--;
                countdownLabel.setText("Thời gian còn lại: " + formatTime(remainingSeconds));
                if (remainingSeconds == 0) {
                    countdownTimer.stop();
                    Maintenance.gI().start(15);
                    messageLabel.setText("");
                    countdownLabel.setText("");
                }
            });
            countdownTimer.start();

            messageLabel.setText("Đã hẹn bảo trì sau " + minutes + " phút");
        } catch (NumberFormatException e) {

            String error = e.getMessage();
            if (error.equals("For input string: \"\"")) {
                JOptionPane.showMessageDialog(null, "Không được để trống");
            } else {
                JOptionPane.showMessageDialog(null, "Bạn nhập sai phút");
            }

        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private String formatUptime(long uptimeMillis) {
        long totalSeconds = uptimeMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void confirmExit() {
        int dialogButton = JOptionPane.YES_NO_OPTION;
        int dialogResult = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn thoát chương trình?", "Thoát", dialogButton);
        if (dialogResult == 0) {
            System.exit(0);
        }
    }

    @Override
    public void setDefaultCloseOperation(int operation) {
        if (operation == JFrame.EXIT_ON_CLOSE) {
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    confirmExit();
                }
            });
        } else {
            super.setDefaultCloseOperation(operation);
        }
    }

    private void scheduleMaintenance(JComboBox<Integer> hoursComboBox, JComboBox<Integer> minutesComboBox, JComboBox<Integer> secondsComboBox) {
        int hours = hoursComboBox.getItemAt(hoursComboBox.getSelectedIndex());
        int minutes = minutesComboBox.getItemAt(minutesComboBox.getSelectedIndex());
        int seconds = secondsComboBox.getItemAt(secondsComboBox.getSelectedIndex());
        if (minutes == -1 || hours == -1 || seconds == -1) {
//            JOptionPane.showMessageDialog(this, "Thời gian sai");
            JOptionPane.showMessageDialog(this, "Chạy sever không cần hẹn bảo trì ?");
            return;
        }
        // Ghi giá trị vào tệp tin
        try ( BufferedWriter writer = new BufferedWriter(new FileWriter("maintenanceConfig.txt"))) {
            writer.write(hours + "\n");
            writer.write(minutes + "\n");
            writer.write(seconds + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AtomicBoolean timeReached = new AtomicBoolean(false); // Sử dụng AtomicBoolean để đảm bảo tính nhất quán trong thread
        info.setText("Đã cài đặt quá trình bảo trì tự động" + " vào lúc " + hours + ":" + minutes + ":" + seconds);
        new Thread(() -> {
            while (!timeReached.get()) { // Kiểm tra điều kiện dừng
                try {
                    LocalTime currentTime = LocalTime.now();
                    int hourss = hoursComboBox.getItemAt(hoursComboBox.getSelectedIndex());
                    int minutess = minutesComboBox.getItemAt(minutesComboBox.getSelectedIndex());
                    int secondss = secondsComboBox.getItemAt(secondsComboBox.getSelectedIndex());
                    int hour_now = currentTime.getHour();
                    int minute_now = currentTime.getMinute();
                    int seconds_now = currentTime.getSecond();

                    if (hourss == hour_now && minutess == minute_now && secondss == seconds_now) {
                        performMaintenance();
                        timeReached.set(true); // Gán giá trị true để dừng vòng lặp
                    }
                    Thread.sleep(10000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private long calculateDelay(int hours, int minutes, int seconds) {
        long currentMillis = System.currentTimeMillis();
        long scheduledMillis = currentMillis + (hours * 60 * 60 * 1000) + (minutes * 60 * 1000) + (seconds * 1000);
        return scheduledMillis - currentMillis;
    }

    private void performMaintenance() {
        Maintenance.gI().start(15);
    }

    private void showEventManagerDialog() {
        JDialog eventDialog = new JDialog(this, "Quản lý Sự kiện", true);
        eventDialog.setSize(400, 500);
        eventDialog.setLocationRelativeTo(this);
        
        JPanel eventPanel = new JPanel();
        eventPanel.setLayout(new BoxLayout(eventPanel, BoxLayout.Y_AXIS));
        
        // Tiêu đề
        JLabel titleLabel = new JLabel("Bật/Tắt Sự kiện");
        titleLabel.setFont(titleLabel.getFont().deriveFont(16f));
        titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        eventPanel.add(titleLabel);
        eventPanel.add(Box.createVerticalStrut(10));
        
        // Checkbox cho các sự kiện
        JCheckBox trungThuCheck = new JCheckBox("Tết Trung Thu", event.EventManager.TRUNG_THU);
        JCheckBox christmasCheck = new JCheckBox("Giáng Sinh", event.EventManager.CHRISTMAS);
        JCheckBox halloweenCheck = new JCheckBox("Halloween", event.EventManager.HALLOWEEN);
        JCheckBox hungVuongCheck = new JCheckBox("Giỗ Tổ Hùng Vương", event.EventManager.HUNG_VUONG);
        JCheckBox lunarNewYearCheck = new JCheckBox("Tết Nguyên Đán", event.EventManager.LUNNAR_NEW_YEAR);
        JCheckBox womenDayCheck = new JCheckBox("Ngày Quốc tế Phụ nữ", event.EventManager.INTERNATIONAL_WOMANS_DAY);
        JCheckBox topUpCheck = new JCheckBox("Sự kiện Nạp thẻ", event.EventManager.TOP_UP);
        
        eventPanel.add(trungThuCheck);
        eventPanel.add(christmasCheck);
        eventPanel.add(halloweenCheck);
        eventPanel.add(hungVuongCheck);
        eventPanel.add(lunarNewYearCheck);
        eventPanel.add(womenDayCheck);
        eventPanel.add(topUpCheck);
        
        eventPanel.add(Box.createVerticalStrut(20));
        
        // Nút áp dụng
        JButton applyButton = new JButton("Áp dụng thay đổi");
        applyButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        applyButton.addActionListener(e -> {
            // Cập nhật trạng thái sự kiện
            event.EventManager.TRUNG_THU = trungThuCheck.isSelected();
            event.EventManager.CHRISTMAS = christmasCheck.isSelected();
            event.EventManager.HALLOWEEN = halloweenCheck.isSelected();
            event.EventManager.HUNG_VUONG = hungVuongCheck.isSelected();
            event.EventManager.LUNNAR_NEW_YEAR = lunarNewYearCheck.isSelected();
            event.EventManager.INTERNATIONAL_WOMANS_DAY = womenDayCheck.isSelected();
            event.EventManager.TOP_UP = topUpCheck.isSelected();
            
            // Khởi tạo lại EventManager
            event.EventManager.gI().reinit();
            
            // Thông báo thành công
            JOptionPane.showMessageDialog(eventDialog, 
                "Đã cập nhật sự kiện thành công!\n" +
                "Thay đổi có hiệu lực ngay lập tức.");
            
            Logger.success("Admin đã cập nhật sự kiện: " +
                "TrungThu=" + event.EventManager.TRUNG_THU + ", " +
                "Christmas=" + event.EventManager.CHRISTMAS + ", " +
                "Halloween=" + event.EventManager.HALLOWEEN + ", " +
                "HungVuong=" + event.EventManager.HUNG_VUONG + ", " +
                "LunarNewYear=" + event.EventManager.LUNNAR_NEW_YEAR + ", " +
                "WomenDay=" + event.EventManager.INTERNATIONAL_WOMANS_DAY + ", " +
                "TopUp=" + event.EventManager.TOP_UP);
            
            eventDialog.dispose();
        });
        
        // Nút đóng
        JButton closeButton = new JButton("Đóng");
        closeButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        closeButton.addActionListener(e -> eventDialog.dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(applyButton);
        buttonPanel.add(closeButton);
        
        eventPanel.add(buttonPanel);
        
        eventDialog.add(eventPanel);
        eventDialog.setVisible(true);
    }
    
    private void showTimeEventDialog() {
        JDialog timeDialog = new JDialog(this, "Quản lý Thời gian Sự kiện Nạp thẻ", true);
        timeDialog.setSize(500, 400);
        timeDialog.setLocationRelativeTo(this);
        
        JPanel timePanel = new JPanel();
        timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.Y_AXIS));
        
        // Tiêu đề
        JLabel titleLabel = new JLabel("Cài đặt Thời gian Sự kiện Nạp thẻ");
        titleLabel.setFont(titleLabel.getFont().deriveFont(16f));
        titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        timePanel.add(titleLabel);
        timePanel.add(Box.createVerticalStrut(20));
        
        // Thông tin hiện tại
        JLabel currentInfoLabel = new JLabel("Thời gian hiện tại:");
        currentInfoLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        timePanel.add(currentInfoLabel);
        
        // Lấy thời gian hiện tại từ ConstDataEventNAP
        String currentTimeText = String.format("Bắt đầu: %d/%d/2025 %d:%02d - Kết thúc: %d/%d/2025 %d:%02d",
            consts.ConstDataEventNAP.DATE_OPEN, consts.ConstDataEventNAP.MONTH_OPEN, 
            consts.ConstDataEventNAP.HOUR_OPEN, consts.ConstDataEventNAP.MIN_OPEN,
            consts.ConstDataEventNAP.DATE_END, consts.ConstDataEventNAP.MONTH_END,
            consts.ConstDataEventNAP.HOUR_END, consts.ConstDataEventNAP.MIN_END);
        
        JLabel currentTimeLabel = new JLabel(currentTimeText);
        currentTimeLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        timePanel.add(currentTimeLabel);
        timePanel.add(Box.createVerticalStrut(20));
        
        // Panel thời gian bắt đầu
        JPanel startPanel = new JPanel();
        startPanel.setLayout(new BoxLayout(startPanel, BoxLayout.X_AXIS));
        startPanel.add(new JLabel("Thời gian bắt đầu:"));
        startPanel.add(Box.createHorizontalStrut(10));
        
        JTextField startMonthField = new JTextField(String.valueOf(consts.ConstDataEventNAP.MONTH_OPEN), 3);
        JTextField startDayField = new JTextField(String.valueOf(consts.ConstDataEventNAP.DATE_OPEN), 3);
        JTextField startHourField = new JTextField(String.valueOf(consts.ConstDataEventNAP.HOUR_OPEN), 3);
        JTextField startMinField = new JTextField(String.valueOf(consts.ConstDataEventNAP.MIN_OPEN), 3);
        
        startPanel.add(new JLabel("Tháng:"));
        startPanel.add(startMonthField);
        startPanel.add(new JLabel("Ngày:"));
        startPanel.add(startDayField);
        startPanel.add(new JLabel("Giờ:"));
        startPanel.add(startHourField);
        startPanel.add(new JLabel("Phút:"));
        startPanel.add(startMinField);
        
        timePanel.add(startPanel);
        timePanel.add(Box.createVerticalStrut(10));
        
        // Panel thời gian kết thúc
        JPanel endPanel = new JPanel();
        endPanel.setLayout(new BoxLayout(endPanel, BoxLayout.X_AXIS));
        endPanel.add(new JLabel("Thời gian kết thúc:"));
        endPanel.add(Box.createHorizontalStrut(10));
        
        JTextField endMonthField = new JTextField(String.valueOf(consts.ConstDataEventNAP.MONTH_END), 3);
        JTextField endDayField = new JTextField(String.valueOf(consts.ConstDataEventNAP.DATE_END), 3);
        JTextField endHourField = new JTextField(String.valueOf(consts.ConstDataEventNAP.HOUR_END), 3);
        JTextField endMinField = new JTextField(String.valueOf(consts.ConstDataEventNAP.MIN_END), 3);
        
        endPanel.add(new JLabel("Tháng:"));
        endPanel.add(endMonthField);
        endPanel.add(new JLabel("Ngày:"));
        endPanel.add(endDayField);
        endPanel.add(new JLabel("Giờ:"));
        endPanel.add(endHourField);
        endPanel.add(new JLabel("Phút:"));
        endPanel.add(endMinField);
        
        timePanel.add(endPanel);
        timePanel.add(Box.createVerticalStrut(20));
        
        // Nút áp dụng
        JButton applyButton = new JButton("Áp dụng thay đổi");
        applyButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        applyButton.addActionListener(e -> {
            try {
                // Lấy giá trị từ các field
                int startMonth = Integer.parseInt(startMonthField.getText());
                int startDay = Integer.parseInt(startDayField.getText());
                int startHour = Integer.parseInt(startHourField.getText());
                int startMin = Integer.parseInt(startMinField.getText());
                
                int endMonth = Integer.parseInt(endMonthField.getText());
                int endDay = Integer.parseInt(endDayField.getText());
                int endHour = Integer.parseInt(endHourField.getText());
                int endMin = Integer.parseInt(endMinField.getText());
                
                // Validate input
                if (startMonth < 1 || startMonth > 12 || endMonth < 1 || endMonth > 12 ||
                    startDay < 1 || startDay > 31 || endDay < 1 || endDay > 31 ||
                    startHour < 0 || startHour > 23 || endHour < 0 || endHour > 23 ||
                    startMin < 0 || startMin > 59 || endMin < 0 || endMin > 59) {
                    JOptionPane.showMessageDialog(timeDialog, "Vui lòng nhập thời gian hợp lệ!");
                    return;
                }
                
                // Cập nhật thời gian trong ConstDataEventNAP
                updateTopUpEventTime(startMonth, startDay, startHour, startMin,
                                   endMonth, endDay, endHour, endMin);
                
                JOptionPane.showMessageDialog(timeDialog, 
                    "Đã cập nhật thời gian sự kiện nạp thẻ thành công!\n" +
                    "Bắt đầu: " + startDay + "/" + startMonth + "/2025 " + startHour + ":" + 
                    String.format("%02d", startMin) + "\n" +
                    "Kết thúc: " + endDay + "/" + endMonth + "/2025 " + endHour + ":" + 
                    String.format("%02d", endMin));
                
                Logger.success("Admin đã cập nhật thời gian sự kiện nạp thẻ: " +
                    "Bắt đầu " + startDay + "/" + startMonth + "/2025 " + startHour + ":" + 
                    String.format("%02d", startMin) + " - " +
                    "Kết thúc " + endDay + "/" + endMonth + "/2025 " + endHour + ":" + 
                    String.format("%02d", endMin));
                
                timeDialog.dispose();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(timeDialog, "Vui lòng nhập số hợp lệ!");
            }
        });
        
        // Nút đóng
        JButton closeButton = new JButton("Đóng");
        closeButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        closeButton.addActionListener(e -> timeDialog.dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(applyButton);
        buttonPanel.add(closeButton);
        
        timePanel.add(buttonPanel);
        
        timeDialog.add(timePanel);
        timeDialog.setVisible(true);
    }
    
    private void updateTopUpEventTime(int startMonth, int startDay, int startHour, int startMin,
                                    int endMonth, int endDay, int endHour, int endMin) {
        try {
            // Gọi method updateEventTime từ ConstDataEventNAP
            consts.ConstDataEventNAP.updateEventTime(startMonth, startDay, startHour, startMin,
                                                   endMonth, endDay, endHour, endMin);
            
            System.out.println("Đã cập nhật thời gian sự kiện nạp thẻ thành công!");
            
        } catch (Exception ex) {
            Logger.error("Lỗi khi cập nhật thời gian sự kiện nạp thẻ: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
