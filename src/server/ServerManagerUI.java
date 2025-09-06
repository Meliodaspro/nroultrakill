package server;

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
import network.SessionManager;
import utils.Logger;

public class ServerManagerUI extends JFrame {

    private JTextField minutesField;
    private JTextField minutesField2;
    private JLabel uptimeLabel;
    private long serverStartTime;
    private ScheduledExecutorService scheduler;

    public ServerManagerUI() {
        setTitle("Chương trình Bảo trì ULTRAKILL SV1");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null);

        // Khởi tạo thời gian bắt đầu server
        serverStartTime = System.currentTimeMillis();

        // Tạo layout chính với BorderLayout
        setLayout(new BorderLayout());
        
        // Panel thông tin server ở trên
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);
        
        // Panel chức năng chính ở giữa
        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);
        
        // Panel thống kê ở dưới
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);

        // Khởi tạo ScheduledExecutorService để cập nhật uptime
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::updateUptime, 0, 1, TimeUnit.SECONDS);

        setVisible(true);
        
        // Hiển thị trạng thái server
        JOptionPane.showMessageDialog(this, "Server đang chạy");
        
        // Khởi động server
        ServerManager.gI().run();
    }
    
    // Panel thông tin server
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Thông tin Server"));
        
        uptimeLabel = new JLabel("Uptime: 00:00:00", JLabel.CENTER);
        panel.add(uptimeLabel);
        
        JLabel statusLabel = new JLabel("Server đang chạy", JLabel.CENTER);
        panel.add(statusLabel);
        
        JLabel threadLabel = new JLabel("Thread: " + Thread.activeCount(), JLabel.CENTER);
        panel.add(threadLabel);
        
        JLabel onlineLabel = new JLabel("Online: 0", JLabel.CENTER);
        panel.add(onlineLabel);
        
        // Cập nhật thống kê real-time
        ScheduledExecutorService threadCountExecutor = Executors.newSingleThreadScheduledExecutor();
        threadCountExecutor.scheduleAtFixedRate(() -> {
            int threadCount = Thread.activeCount();
            threadLabel.setText("Thread: " + threadCount);
        }, 1, 1, TimeUnit.SECONDS);

        ScheduledExecutorService plCountExecutor = Executors.newSingleThreadScheduledExecutor();
        plCountExecutor.scheduleAtFixedRate(() -> {
            int plcount = server.Client.gI().getPlayers().size();
            onlineLabel.setText("Online: " + plcount);
        }, 5, 1, TimeUnit.SECONDS);
        
        return panel;
    }
    
    // Panel chức năng chính
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Chức năng chính"));
        
        // Bảo trì
        JPanel maintenancePanel = new JPanel(new FlowLayout());
        maintenancePanel.setBorder(BorderFactory.createTitledBorder("Bảo trì (giây)"));
        
        JLabel label1 = new JLabel("Giây:");
        minutesField = new JTextField("15", 5);
        JButton maintenanceButton = new JButton("Bảo trì ngay");
        maintenanceButton.addActionListener(e -> {
            try {
                int seconds = Integer.parseInt(minutesField.getText());
                if (seconds > 0) {
                    int dialogButton = JOptionPane.YES_NO_OPTION;
                    int dialogResult = JOptionPane.showConfirmDialog(this, "Bắt đầu bảo trì sau " + seconds + " giây?", "Bảo trì", dialogButton);
                    if (dialogResult == 0) {
                        Logger.error("Server tiến hành bảo trì sau " + seconds + " giây");
                        Maintenance.gI().start(seconds);
                        JOptionPane.showMessageDialog(this, "Bảo trì đã được lên lịch trong " + seconds + " giây");
                    } else {
                        System.out.println("No Option");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Vui lòng nhập số giây hợp lệ");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập số hợp lệ");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        maintenancePanel.add(label1);
        maintenancePanel.add(minutesField);
        maintenancePanel.add(maintenanceButton);
        
        // Hẹn giờ bảo trì
        JPanel schedulePanel = new JPanel(new FlowLayout());
        schedulePanel.setBorder(BorderFactory.createTitledBorder("Hẹn giờ"));
        
        JLabel label2 = new JLabel("Giờ:");
        // Đọc giá trị từ tệp tin maintenanceConfig.txt
        int hours = -1, minutes = -1, seconds = -1;
        try (BufferedReader reader = new BufferedReader(new FileReader("maintenanceConfig.txt"))) {
            String hoursLine = reader.readLine();
            String minutesLine = reader.readLine();
            String secondsLine = reader.readLine();
            
            if (hoursLine != null && minutesLine != null && secondsLine != null) {
                hours = Integer.parseInt(hoursLine);
                minutes = Integer.parseInt(minutesLine);
                seconds = Integer.parseInt(secondsLine);
            }
        } catch (IOException e) {
            // Sử dụng giá trị mặc định nếu không đọc được file
            hours = -1; minutes = -1; seconds = -1;
        }
        
        // ComboBox cho giờ
        DefaultComboBoxModel<Integer> hoursModel = new DefaultComboBoxModel<>();
        for (int i = -1; i < 24; i++) {
            hoursModel.addElement(i);
        }
        JComboBox<Integer> hoursComboBox = new JComboBox<>(hoursModel);
        hoursComboBox.setSelectedItem(hours);
        schedulePanel.add(label2);
        schedulePanel.add(hoursComboBox);
        
        JLabel label3 = new JLabel("Phút:");
        DefaultComboBoxModel<Integer> minutesModel = new DefaultComboBoxModel<>();
        for (int i = -1; i < 60; i++) {
            minutesModel.addElement(i);
        }
        JComboBox<Integer> minutesComboBox = new JComboBox<>(minutesModel);
        minutesComboBox.setSelectedItem(minutes);
        schedulePanel.add(label3);
        schedulePanel.add(minutesComboBox);
        
        JLabel label4 = new JLabel("Giây:");
        DefaultComboBoxModel<Integer> secondsModel = new DefaultComboBoxModel<>();
        for (int i = -1; i < 60; i++) {
            secondsModel.addElement(i);
        }
        JComboBox<Integer> secondsComboBox = new JComboBox<>(secondsModel);
        secondsComboBox.setSelectedItem(seconds);
        schedulePanel.add(label4);
        schedulePanel.add(secondsComboBox);
        
        JButton scheduleButton = new JButton("Hẹn giờ");
        scheduleButton.addActionListener(e -> scheduleMaintenance(hoursComboBox, minutesComboBox, secondsComboBox));
        schedulePanel.add(scheduleButton);
        
        // Tự động hẹn giờ nếu đã có cài đặt
        if (hours != -1 && minutes != -1 && seconds != -1) {
            scheduleMaintenance(hoursComboBox, minutesComboBox, secondsComboBox);
        }
        
        // Quản lý sự kiện
        JPanel eventPanel = new JPanel(new FlowLayout());
        eventPanel.setBorder(BorderFactory.createTitledBorder("Sự kiện"));
        
        JButton eventButton = new JButton("Quản lý Sự kiện");
        eventButton.addActionListener(e -> showEventManagerDialog());
        eventPanel.add(eventButton);
        
        // Thời gian sự kiện nạp thẻ
        JPanel timeEventPanel = new JPanel(new FlowLayout());
        timeEventPanel.setBorder(BorderFactory.createTitledBorder("Sự kiện Nạp thẻ"));
        
        JButton timeEventButton = new JButton("Thời gian Nạp thẻ");
        timeEventButton.addActionListener(e -> showTimeEventDialog());
        timeEventPanel.add(timeEventButton);
        
        // Thời gian sự kiện top sức mạnh
        JPanel timeSMPanel = new JPanel(new FlowLayout());
        timeSMPanel.setBorder(BorderFactory.createTitledBorder("Sự kiện Top SM"));
        
        JButton timeSMButton = new JButton("Thời gian Top SM");
        timeSMButton.addActionListener(e -> showTimeSMDialog());
        timeSMPanel.add(timeSMButton);
        
        // Chức năng khác
        JPanel otherPanel = new JPanel(new FlowLayout());
        otherPanel.setBorder(BorderFactory.createTitledBorder("Chức năng khác"));
        
        JButton saveButton = new JButton("Lưu Data");
        saveButton.addActionListener(e -> {
            Logger.success("Đang tiến hành lưu data");
            network.server.EMTIServer.gI().stopConnect();

            Maintenance.isRunning = false;
            try {
                Logger.error("Đang tiến hành lưu data bang hội");
                services.ClanService.gI().close();
                Thread.sleep(1000);
                Logger.success("Lưu dữ liệu bang hội thành công");
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.error("Lỗi lưu dữ liệu bang hội");
            }
            try {
                Logger.error("Đang tiến hành lưu data ký gửi");
                models.Consign.ConsignShopManager.gI().save();
                Thread.sleep(1000);
                Logger.success("Lưu dữ liệu ký gửi thành công");
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.error("Lỗi lưu dữ liệu ký gửi");
            }

            try {
                Logger.error("Đang tiến hành đẩy người chơi");
                server.Client.gI().close();
                jdbc.daos.EventDAO.save();
                Thread.sleep(1000);
                Logger.success("Lưu dữ liệu người dùng thành công");
            } catch (Exception ex) {
                ex.printStackTrace();
                Logger.error("Lỗi lưu dữ liệu người dùng");
            }
            System.exit(0);
        });
        
        JButton clearFwButton = new JButton("Clear Firewall");
        clearFwButton.addActionListener(e -> {
            try {
                int count = server.io.MySession.getAntiLoginCount();
                server.io.MySession.clearAntiLogin();
                JOptionPane.showMessageDialog(this, 
                    "Đã xóa " + count + " bản ghi anti-login thành công!");
                utils.Logger.success("Admin đã xóa " + count + " bản ghi anti-login");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi khi xóa anti-login: " + ex.getMessage());
                utils.Logger.error("Lỗi khi xóa anti-login: " + ex.getMessage());
            }
        });
        
        otherPanel.add(saveButton);
        otherPanel.add(clearFwButton);
        
        // Thêm các panel vào layout
        panel.add(maintenancePanel);
        panel.add(schedulePanel);
        panel.add(eventPanel);
        panel.add(timeEventPanel);
        panel.add(timeSMPanel);
        panel.add(otherPanel);
        
        return panel;
    }
    
    // Panel thống kê
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Thống kê"));
        
        JLabel sessionLabel = new JLabel("Session: 0", JLabel.CENTER);
        panel.add(sessionLabel);
        
        JLabel memoryLabel = new JLabel("Memory: " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB", JLabel.CENTER);
        panel.add(memoryLabel);
        
        JLabel freeMemoryLabel = new JLabel("Free: " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB", JLabel.CENTER);
        panel.add(freeMemoryLabel);
        
        // Cập nhật session count real-time
        ScheduledExecutorService ssCountExecutor = Executors.newSingleThreadScheduledExecutor();
        ssCountExecutor.scheduleAtFixedRate(() -> {
            int sscount = SessionManager.gI().getSessions().size();
            sessionLabel.setText("Session: " + sscount);
        }, 5, 1, TimeUnit.SECONDS);
        
        return panel;
    }

    private void updateUptime() {
        long uptimeMillis = System.currentTimeMillis() - serverStartTime;
        String uptime = formatUptime(uptimeMillis);
        SwingUtilities.invokeLater(() -> {
            if (uptimeLabel != null) {
                uptimeLabel.setText("Uptime: " + uptime);
            }
        });
    }

    private String formatUptime(long uptimeMillis) {
        long seconds = uptimeMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds %= 60;
        minutes %= 60;
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // Method để hiển thị dialog quản lý sự kiện
    private void showEventManagerDialog() {
        JDialog eventDialog = new JDialog((JFrame) null, "Quản lý Sự kiện", true);
        eventDialog.setSize(400, 500);
        eventDialog.setLocationRelativeTo(null);
        
        JPanel eventPanel = new JPanel();
        eventPanel.setLayout(new BoxLayout(eventPanel, BoxLayout.Y_AXIS));
        eventPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Tiêu đề
        JLabel titleLabel = new JLabel("Quản lý Sự kiện");
        titleLabel.setFont(titleLabel.getFont().deriveFont(16f));
        titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        eventPanel.add(titleLabel);
        eventPanel.add(Box.createVerticalStrut(20));
        
        // Checkbox cho các sự kiện
        JCheckBox trungThuCheck = new JCheckBox("Trung Thu");
        JCheckBox christmasCheck = new JCheckBox("Christmas");
        JCheckBox halloweenCheck = new JCheckBox("Halloween");
        JCheckBox hungVuongCheck = new JCheckBox("Hung Vuong");
        JCheckBox lunarNewYearCheck = new JCheckBox("Lunar New Year");
        JCheckBox internationalWomensDayCheck = new JCheckBox("International Women's Day");
        JCheckBox topUpCheck = new JCheckBox("Top Up");
        
        // Load trạng thái hiện tại
        trungThuCheck.setSelected(event.EventManager.TRUNG_THU);
        christmasCheck.setSelected(event.EventManager.CHRISTMAS);
        halloweenCheck.setSelected(event.EventManager.HALLOWEEN);
        hungVuongCheck.setSelected(event.EventManager.HUNG_VUONG);
        lunarNewYearCheck.setSelected(event.EventManager.LUNNAR_NEW_YEAR);
        internationalWomensDayCheck.setSelected(event.EventManager.INTERNATIONAL_WOMANS_DAY);
        topUpCheck.setSelected(event.EventManager.TOP_UP);
        
        eventPanel.add(trungThuCheck);
        eventPanel.add(christmasCheck);
        eventPanel.add(halloweenCheck);
        eventPanel.add(hungVuongCheck);
        eventPanel.add(lunarNewYearCheck);
        eventPanel.add(internationalWomensDayCheck);
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
            event.EventManager.INTERNATIONAL_WOMANS_DAY = internationalWomensDayCheck.isSelected();
            event.EventManager.TOP_UP = topUpCheck.isSelected();
            
            // Reinit sự kiện
            event.EventManager.gI().reinit();
            
            JOptionPane.showMessageDialog(eventDialog, "Đã cập nhật trạng thái sự kiện thành công!");
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

    // Method để hiển thị dialog quản lý thời gian sự kiện nạp thẻ
    private void showTimeEventDialog() {
        JDialog timeDialog = new JDialog((JFrame) null, "Quản lý Thời gian Sự kiện Nạp thẻ", true);
        timeDialog.setSize(500, 400);
        timeDialog.setLocationRelativeTo(null);
        
        JPanel timePanel = new JPanel();
        timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.Y_AXIS));
        timePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
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
        
        // Lấy thời gian hiện tại từ file config
        int[] currentTimes = getCurrentEventTimesFromConfig();
        String currentTimeText = String.format("Bắt đầu: %d/%d/2025 %d:%02d - Kết thúc: %d/%d/2025 %d:%02d",
            currentTimes[1], currentTimes[0], currentTimes[2], currentTimes[3],  // start: day, month, hour, min
            currentTimes[5], currentTimes[4], currentTimes[6], currentTimes[7]); // end: day, month, hour, min
        
        JLabel currentTimeLabel = new JLabel(currentTimeText);
        currentTimeLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        timePanel.add(currentTimeLabel);
        timePanel.add(Box.createVerticalStrut(20));
        
        // Panel thời gian bắt đầu
        JPanel startPanel = new JPanel();
        startPanel.setLayout(new BoxLayout(startPanel, BoxLayout.X_AXIS));
        startPanel.add(new JLabel("Thời gian bắt đầu:"));
        startPanel.add(Box.createHorizontalStrut(10));
        
        JTextField startMonthField = new JTextField(String.valueOf(currentTimes[0]), 3);
        JTextField startDayField = new JTextField(String.valueOf(currentTimes[1]), 3);
        JTextField startHourField = new JTextField(String.valueOf(currentTimes[2]), 3);
        JTextField startMinField = new JTextField(String.valueOf(currentTimes[3]), 3);
        
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
        
        JTextField endMonthField = new JTextField(String.valueOf(currentTimes[4]), 3);
        JTextField endDayField = new JTextField(String.valueOf(currentTimes[5]), 3);
        JTextField endHourField = new JTextField(String.valueOf(currentTimes[6]), 3);
        JTextField endMinField = new JTextField(String.valueOf(currentTimes[7]), 3);
        
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
            
            // Force reload để đảm bảo sync
            consts.ConstDataEventNAP.reloadEventTimeFromConfig();
            
            System.out.println("Đã cập nhật thời gian sự kiện nạp thẻ thành công!");
            
        } catch (Exception ex) {
            Logger.error("Lỗi khi cập nhật thời gian sự kiện nạp thẻ: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    // Method để đọc thời gian hiện tại từ file config cho sự kiện nạp thẻ
    private int[] getCurrentEventTimesFromConfig() {
        int[] times = new int[8]; // [startMonth, startDay, startHour, startMin, endMonth, endDay, endHour, endMin]
        
        try {
            java.util.Properties props = new java.util.Properties();
            java.io.File configFile = new java.io.File("data/config/config.properties");
            
            if (configFile.exists()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
                    props.load(fis);
                    
                    // Đọc thời gian bắt đầu
                    times[0] = Integer.parseInt(props.getProperty("nap_event.start_month", "0"));
                    times[1] = Integer.parseInt(props.getProperty("nap_event.start_day", "0"));
                    times[2] = Integer.parseInt(props.getProperty("nap_event.start_hour", "0"));
                    times[3] = Integer.parseInt(props.getProperty("nap_event.start_min", "0"));
                    
                    // Đọc thời gian kết thúc
                    times[4] = Integer.parseInt(props.getProperty("nap_event.end_month", "0"));
                    times[5] = Integer.parseInt(props.getProperty("nap_event.end_day", "0"));
                    times[6] = Integer.parseInt(props.getProperty("nap_event.end_hour", "0"));
                    times[7] = Integer.parseInt(props.getProperty("nap_event.end_min", "0"));
                }
            } else {
                // Fallback về giá trị mặc định nếu file không tồn tại
                times[0] = 0; times[1] = 0; times[2] = 0; times[3] = 0;
                times[4] = 0; times[5] = 0; times[6] = 0; times[7] = 0;
            }
        } catch (Exception e) {
            // Fallback về giá trị mặc định nếu có lỗi
            times[0] = 0; times[1] = 0; times[2] = 0; times[3] = 0;
            times[4] = 0; times[5] = 0; times[6] = 0; times[7] = 0;
            System.out.println("Lỗi khi đọc config: " + e.getMessage());
        }
        
        return times;
    }
    
    // Method để hiển thị dialog quản lý thời gian sự kiện top sức mạnh
    private void showTimeSMDialog() {
        JDialog timeDialog = new JDialog((JFrame) null, "Quản lý Thời gian Sự kiện Top Sức mạnh", true);
        timeDialog.setSize(500, 400);
        timeDialog.setLocationRelativeTo(null);
        
        JPanel timePanel = new JPanel();
        timePanel.setLayout(new BoxLayout(timePanel, BoxLayout.Y_AXIS));
        timePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Tiêu đề
        JLabel titleLabel = new JLabel("Cài đặt Thời gian Sự kiện Top Sức mạnh");
        titleLabel.setFont(titleLabel.getFont().deriveFont(16f));
        titleLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        timePanel.add(titleLabel);
        timePanel.add(Box.createVerticalStrut(20));
        
        // Thông tin hiện tại
        JLabel currentInfoLabel = new JLabel("Thời gian hiện tại:");
        currentInfoLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        timePanel.add(currentInfoLabel);
        
        // Lấy thời gian hiện tại từ file config
        int[] currentTimes = getCurrentSMTimesFromConfig();
        String currentTimeText = String.format("Bắt đầu: %d/%d/2025 %d:%02d - Kết thúc: %d/%d/2025 %d:%02d",
            currentTimes[1], currentTimes[0], currentTimes[2], currentTimes[3],  // start: day, month, hour, min
            currentTimes[5], currentTimes[4], currentTimes[6], currentTimes[7]); // end: day, month, hour, min
        
        JLabel currentTimeLabel = new JLabel(currentTimeText);
        currentTimeLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        timePanel.add(currentTimeLabel);
        timePanel.add(Box.createVerticalStrut(20));
        
        // Panel thời gian bắt đầu
        JPanel startPanel = new JPanel();
        startPanel.setLayout(new BoxLayout(startPanel, BoxLayout.X_AXIS));
        startPanel.add(new JLabel("Thời gian bắt đầu:"));
        startPanel.add(Box.createHorizontalStrut(10));
        
        JTextField startMonthField = new JTextField(String.valueOf(currentTimes[0]), 3);
        JTextField startDayField = new JTextField(String.valueOf(currentTimes[1]), 3);
        JTextField startHourField = new JTextField(String.valueOf(currentTimes[2]), 3);
        JTextField startMinField = new JTextField(String.valueOf(currentTimes[3]), 3);
        
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
        
        JTextField endMonthField = new JTextField(String.valueOf(currentTimes[4]), 3);
        JTextField endDayField = new JTextField(String.valueOf(currentTimes[5]), 3);
        JTextField endHourField = new JTextField(String.valueOf(currentTimes[6]), 3);
        JTextField endMinField = new JTextField(String.valueOf(currentTimes[7]), 3);
        
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
                
                // Cập nhật thời gian trong ConstDataEventSM
                updateTopSMEventTime(startMonth, startDay, startHour, startMin,
                                   endMonth, endDay, endHour, endMin);
                
                JOptionPane.showMessageDialog(timeDialog, 
                    "Đã cập nhật thời gian sự kiện top sức mạnh thành công!\n" +
                    "Bắt đầu: " + startDay + "/" + startMonth + "/2025 " + startHour + ":" + 
                    String.format("%02d", startMin) + "\n" +
                    "Kết thúc: " + endDay + "/" + endMonth + "/2025 " + endHour + ":" + 
                    String.format("%02d", endMin));
                
                Logger.success("Admin đã cập nhật thời gian sự kiện top sức mạnh: " +
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
    
    private void updateTopSMEventTime(int startMonth, int startDay, int startHour, int startMin,
                                    int endMonth, int endDay, int endHour, int endMin) {
        try {
            // Gọi method updateEventTime từ ConstDataEventSM
            consts.ConstDataEventSM.updateEventTime(startMonth, startDay, startHour, startMin,
                                                   endMonth, endDay, endHour, endMin);
            
            // Force reload để đảm bảo sync
            consts.ConstDataEventSM.reloadEventTimeFromConfig();
            
            System.out.println("Đã cập nhật thời gian sự kiện top sức mạnh thành công!");
            
        } catch (Exception ex) {
            Logger.error("Lỗi khi cập nhật thời gian sự kiện top sức mạnh: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
    
    // Method để đọc thời gian hiện tại từ file config cho sự kiện top sức mạnh
    private int[] getCurrentSMTimesFromConfig() {
        int[] times = new int[8]; // [startMonth, startDay, startHour, startMin, endMonth, endDay, endHour, endMin]
        
        try {
            java.util.Properties props = new java.util.Properties();
            java.io.File configFile = new java.io.File("data/config/config.properties");
            
            if (configFile.exists()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
                    props.load(fis);
                    
                    // Đọc thời gian bắt đầu
                    times[0] = Integer.parseInt(props.getProperty("sm_event.start_month", "0"));
                    times[1] = Integer.parseInt(props.getProperty("sm_event.start_day", "0"));
                    times[2] = Integer.parseInt(props.getProperty("sm_event.start_hour", "0"));
                    times[3] = Integer.parseInt(props.getProperty("sm_event.start_min", "0"));
                    
                    // Đọc thời gian kết thúc
                    times[4] = Integer.parseInt(props.getProperty("sm_event.end_month", "0"));
                    times[5] = Integer.parseInt(props.getProperty("sm_event.end_day", "0"));
                    times[6] = Integer.parseInt(props.getProperty("sm_event.end_hour", "0"));
                    times[7] = Integer.parseInt(props.getProperty("sm_event.end_min", "0"));
                }
            } else {
                // Fallback về giá trị mặc định nếu file không tồn tại
                times[0] = 0; times[1] = 0; times[2] = 0; times[3] = 0;
                times[4] = 0; times[5] = 0; times[6] = 0; times[7] = 0;
            }
        } catch (Exception e) {
            // Fallback về giá trị mặc định nếu có lỗi
            times[0] = 0; times[1] = 0; times[2] = 0; times[3] = 0;
            times[4] = 0; times[5] = 0; times[6] = 0; times[7] = 0;
            System.out.println("Lỗi khi đọc config: " + e.getMessage());
        }
        
        return times;
    }
    
    // Method để hẹn giờ bảo trì (từ file cũ)
    private void scheduleMaintenance(JComboBox<Integer> hoursComboBox, JComboBox<Integer> minutesComboBox, JComboBox<Integer> secondsComboBox) {
        int hours = hoursComboBox.getItemAt(hoursComboBox.getSelectedIndex());
        int minutes = minutesComboBox.getItemAt(minutesComboBox.getSelectedIndex());
        int seconds = secondsComboBox.getItemAt(secondsComboBox.getSelectedIndex());
        
        if (minutes == -1 || hours == -1 || seconds == -1) {
            JOptionPane.showMessageDialog(this, "Chạy sever không cần hẹn bảo trì ?");
            return;
        }
        
        // Ghi giá trị vào tệp tin
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("maintenanceConfig.txt"))) {
            writer.write(hours + "\n");
            writer.write(minutes + "\n");
            writer.write(seconds + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        AtomicBoolean timeReached = new AtomicBoolean(false);
        JOptionPane.showMessageDialog(this, "Đã cài đặt quá trình bảo trì tự động vào lúc " + hours + ":" + minutes + ":" + seconds + " (bảo trì 15 giây)");
        
        new Thread(() -> {
            while (!timeReached.get()) {
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
                        timeReached.set(true);
                    }
                    Thread.sleep(10000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    private void performMaintenance() {
        Maintenance.gI().start(15); // 15 giây
    }
}
