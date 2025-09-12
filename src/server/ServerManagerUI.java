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
import network.server.EmtiSessionManager;
import utils.Logger;
import utils.Threading;

public class ServerManagerUI extends JFrame {

    private JTextField minutesField;
    private JTextField minutesField2;
    private JLabel uptimeLabel;
    private long serverStartTime;
    private ScheduledExecutorService scheduler;
    // Labels thông tin dưới các nhóm chức năng
    private JLabel napEventInfoLabel;
    private JLabel smEventInfoLabel;
    private JLabel activeEventsLabel;

    public ServerManagerUI() {
        setTitle("Chương trình Bảo trì ULTRAKILL SV1");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);

        // Khởi tạo thời gian bắt đầu server
        serverStartTime = System.currentTimeMillis();

        // Tạo layout chính với BorderLayout
        setLayout(new BorderLayout());
        
        // Panel thông tin server ở trên
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);
        
        // Tạo tabbed pane cho các chức năng
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));
        
        // Tab 1: Quản lý Server
        tabbedPane.addTab("Quản lý Server", createServerManagementTab());
        
        // Tab 2: Cấu hình
        tabbedPane.addTab("Cấu hình", createConfigTab());
        
        // Tab 3: Sự kiện
        tabbedPane.addTab("Sự kiện", createEventTab());
        
        // Tab 4: Thống kê
        tabbedPane.addTab("Thống kê", createStatsPanel());

        add(tabbedPane, BorderLayout.CENTER);

        // Khởi tạo ScheduledExecutorService để cập nhật uptime
        scheduler = Threading.scheduler();
        scheduler.scheduleAtFixedRate(this::updateUptime, 0, 1, TimeUnit.SECONDS);

        setVisible(true);
        
        // Khởi động server trong thread riêng để tránh lag panel
        Threading.runAsync(() -> {
            try {
                ServerManager.gI().run();
            } catch (Exception e) {
                Logger.error("Lỗi khởi động server: " + e.getMessage());
                e.printStackTrace();
            }
        });
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
        Threading.scheduler().scheduleAtFixedRate(() -> {
            int threadCount = Thread.activeCount();
            threadLabel.setText("Thread: " + threadCount);
        }, 1, 1, TimeUnit.SECONDS);

        Threading.scheduler().scheduleAtFixedRate(() -> {
            int plcount = server.Client.gI().getPlayers().size();
            onlineLabel.setText("Online: " + plcount);
        }, 5, 1, TimeUnit.SECONDS);
        
        return panel;
    }
    
    // Tab Quản lý Server
    private JPanel createServerManagementTab() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Bảo trì
        JPanel maintenancePanel = new JPanel(new FlowLayout());
        maintenancePanel.setBorder(BorderFactory.createTitledBorder("Bảo trì (giây)"));
        
        JLabel label1 = new JLabel("Giây:");
        minutesField = new JTextField("15", 5);
        JButton maintenanceButton = new JButton("Bảo trì ngay");
        maintenanceButton.setPreferredSize(new Dimension(100, 35));
        maintenanceButton.setFont(new Font("Arial", Font.PLAIN, 11));
        maintenanceButton.addActionListener(e -> {
            try {
                int seconds = Integer.parseInt(minutesField.getText());
                if (seconds > 0) {
                    int dialogButton = JOptionPane.YES_NO_OPTION;
                    int dialogResult = JOptionPane.showConfirmDialog(this, "Bắt đầu bảo trì sau " + seconds + " giây?", "Bảo trì", dialogButton);
                    if (dialogResult == 0) {
                        // Chạy bảo trì trong thread riêng để tránh lag panel
                        Threading.runAsync(() -> {
                            try {
                                Logger.error("Server tiến hành bảo trì sau " + seconds + " giây");
                                Maintenance.gI().start(seconds);
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(this, "Bảo trì đã được lên lịch trong " + seconds + " giây");
                                });
                            } catch (Exception ex) {
                                Logger.error("Lỗi thực hiện bảo trì: " + ex.getMessage());
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(this, "Lỗi thực hiện bảo trì: " + ex.getMessage());
                                });
                            }
                        });
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
        hoursComboBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String text = String.valueOf(value);
                try {
                    int v = Integer.parseInt(text);
                    if (v >= 0 && v < 10) text = "0" + v; // hiển thị 2 chữ số
                } catch (Exception ignored) {}
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
        schedulePanel.add(label2);
        schedulePanel.add(hoursComboBox);
        
        JLabel label3 = new JLabel("Phút:");
        DefaultComboBoxModel<Integer> minutesModel = new DefaultComboBoxModel<>();
        for (int i = -1; i < 60; i++) {
            minutesModel.addElement(i);
        }
        JComboBox<Integer> minutesComboBox = new JComboBox<>(minutesModel);
        minutesComboBox.setSelectedItem(minutes);
        minutesComboBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String text = String.valueOf(value);
                try {
                    int v = Integer.parseInt(text);
                    if (v >= 0 && v < 10) text = "0" + v;
                } catch (Exception ignored) {}
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
        schedulePanel.add(label3);
        schedulePanel.add(minutesComboBox);
        
        JLabel label4 = new JLabel("Giây:");
        DefaultComboBoxModel<Integer> secondsModel = new DefaultComboBoxModel<>();
        for (int i = -1; i < 60; i++) {
            secondsModel.addElement(i);
        }
        JComboBox<Integer> secondsComboBox = new JComboBox<>(secondsModel);
        secondsComboBox.setSelectedItem(seconds);
        secondsComboBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String text = String.valueOf(value);
                try {
                    int v = Integer.parseInt(text);
                    if (v >= 0 && v < 10) text = "0" + v;
                } catch (Exception ignored) {}
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
        schedulePanel.add(label4);
        schedulePanel.add(secondsComboBox);
        
        JButton scheduleButton = new JButton("Hẹn giờ");
        scheduleButton.setPreferredSize(new Dimension(100, 35));
        scheduleButton.setFont(new Font("Arial", Font.PLAIN, 11));
        scheduleButton.addActionListener(e -> scheduleMaintenance(hoursComboBox, minutesComboBox, secondsComboBox, true));
        schedulePanel.add(scheduleButton);
        
        // Tự động hẹn giờ nếu đã có cài đặt
        if (hours != -1 && minutes != -1 && seconds != -1) {
            scheduleMaintenance(hoursComboBox, minutesComboBox, secondsComboBox, false);
        }
        
        
        // Chức năng khác
        JPanel otherPanel = new JPanel(new FlowLayout());
        otherPanel.setBorder(BorderFactory.createTitledBorder("Chức năng khác"));
        
        JButton saveButton = new JButton("Lưu Data");
        saveButton.setPreferredSize(new Dimension(100, 35));
        saveButton.setFont(new Font("Arial", Font.PLAIN, 11));
        saveButton.addActionListener(e -> {
            // Chạy trong thread riêng để tránh lag panel
            Threading.runAsync(() -> {
                try {
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
                } catch (Exception ex) {
                    Logger.error("Lỗi trong quá trình lưu data: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        });
        
        JButton clearFwButton = new JButton("Clear Firewall");
        clearFwButton.addActionListener(e -> {
            // Chạy trong thread riêng để tránh lag panel
            Threading.runAsync(() -> {
                try {
                    int count = server.io.MySession.getAntiLoginCount();
                    server.io.MySession.clearAntiLogin();
                    utils.Logger.success("Admin đã xóa " + count + " bản ghi anti-login");
                    
                    // Cập nhật UI trong EDT thread
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, 
                            "Đã xóa " + count + " bản ghi anti-login thành công!");
                    });
                } catch (Exception ex) {
                    utils.Logger.error("Lỗi khi xóa anti-login: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Lỗi khi xóa anti-login: " + ex.getMessage());
                    });
                }
            });
        });
        
        otherPanel.add(saveButton);
        otherPanel.add(clearFwButton);
        
        // Panel cấu hình server
        JPanel serverConfigPanel = createServerConfigPanel();
        
        // Panel quản lý data versions
        JPanel dataVersionPanel = createDataVersionPanel();
        
        // Thêm các panel vào layout
        panel.add(maintenancePanel);
        panel.add(schedulePanel);
        panel.add(otherPanel);
        
        return panel;
    }
    
    // Tab Cấu hình
    private JPanel createConfigTab() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Panel cấu hình server
        JPanel serverConfigPanel = createServerConfigPanel();
        serverConfigPanel.setBorder(BorderFactory.createTitledBorder("Cấu hình Server"));
        
        // Panel data versions
        JPanel dataVersionPanel = createDataVersionPanel();
        dataVersionPanel.setBorder(BorderFactory.createTitledBorder("Quản lý Data Versions"));
        
        panel.add(serverConfigPanel);
        panel.add(dataVersionPanel);
        
        return panel;
    }
    
    // Tab Sự kiện
    private JPanel createEventTab() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Sự kiện
        JPanel eventPanel = new JPanel(new FlowLayout());
        eventPanel.setBorder(BorderFactory.createTitledBorder("Sự kiện"));
        JButton eventButton = new JButton("Quản lý Sự kiện");
        eventButton.setPreferredSize(new Dimension(120, 35));
        eventButton.setFont(new Font("Arial", Font.PLAIN, 11));
        eventButton.addActionListener(e -> {
            showEventManagerDialog();
            updateActiveEventsLabel();
        });
        eventPanel.add(eventButton);
        activeEventsLabel = new JLabel();
        updateActiveEventsLabel();
        eventPanel.add(activeEventsLabel);
        
        // Sự kiện Nạp thẻ
        JPanel napEventPanel = new JPanel(new FlowLayout());
        napEventPanel.setBorder(BorderFactory.createTitledBorder("Sự kiện Nạp thẻ"));
        JButton napEventButton = new JButton("Thời gian Nạp thẻ");
        napEventButton.setPreferredSize(new Dimension(120, 35));
        napEventButton.setFont(new Font("Arial", Font.PLAIN, 11));
        napEventInfoLabel = new JLabel();
        updateNapEventInfoLabel(napEventInfoLabel);
        napEventButton.addActionListener(e -> {
            showTimeEventDialog();
            // cập nhật lại thông tin ngay sau khi cấu hình thay đổi
            updateNapEventInfoLabel(napEventInfoLabel);
        });
        napEventPanel.add(napEventButton);
        napEventPanel.add(napEventInfoLabel);
        
        // Sự kiện Top SM
        JPanel smEventPanel = new JPanel(new FlowLayout());
        smEventPanel.setBorder(BorderFactory.createTitledBorder("Sự kiện Top SM"));
        JButton smEventButton = new JButton("Thời gian Top SM");
        smEventButton.setPreferredSize(new Dimension(120, 35));
        smEventButton.setFont(new Font("Arial", Font.PLAIN, 11));
        smEventInfoLabel = new JLabel();
        updateSMEventInfoLabel(smEventInfoLabel);
        smEventButton.addActionListener(e -> {
            showTimeSMDialog();
            // cập nhật lại thông tin ngay sau khi cấu hình thay đổi
            updateSMEventInfoLabel(smEventInfoLabel);
        });
        smEventPanel.add(smEventButton);
        smEventPanel.add(smEventInfoLabel);
        
        // Chức năng khác
        JPanel otherPanel = new JPanel(new FlowLayout());
        otherPanel.setBorder(BorderFactory.createTitledBorder("Chức năng khác"));
        JButton saveDataButton = new JButton("Lưu Data");
        saveDataButton.setPreferredSize(new Dimension(100, 35));
        saveDataButton.setFont(new Font("Arial", Font.PLAIN, 11));
        JLabel otherInfoLabel = new JLabel("Chưa thực hiện");
        saveDataButton.addActionListener(e -> {
            // TODO: Implement save data
            JOptionPane.showMessageDialog(this, "Chức năng lưu data đang được phát triển!");
            otherInfoLabel.setText("Đã lưu dữ liệu lúc: " + java.time.LocalTime.now().withNano(0));
        });
        JButton clearFirewallButton = new JButton("Clear Firewall");
        clearFirewallButton.setPreferredSize(new Dimension(100, 35));
        clearFirewallButton.setFont(new Font("Arial", Font.PLAIN, 11));
        clearFirewallButton.addActionListener(e -> {
            // TODO: Implement clear firewall
            JOptionPane.showMessageDialog(this, "Chức năng clear firewall đang được phát triển!");
            otherInfoLabel.setText("Đã clear firewall lúc: " + java.time.LocalTime.now().withNano(0));
        });
        otherPanel.add(saveDataButton);
        otherPanel.add(clearFirewallButton);
        otherPanel.add(otherInfoLabel);
        
        panel.add(eventPanel);
        panel.add(napEventPanel);
        panel.add(smEventPanel);
        panel.add(otherPanel);
        
        return panel;
    }

    // Cập nhật label thông tin Nạp thẻ
    private void updateNapEventInfoLabel(JLabel label) {
        try {
            java.util.Properties props = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream("data/config/config.properties")) {
                props.load(fis);
            }
            String start = String.format("%s/%s %s:%s",
                    props.getProperty("nap_event.start_day", "15"),
                    props.getProperty("nap_event.start_month", "3"),
                    props.getProperty("nap_event.start_hour", "9"),
                    props.getProperty("nap_event.start_min", "0"));
            String end = String.format("%s/%s %s:%s",
                    props.getProperty("nap_event.end_day", "31"),
                    props.getProperty("nap_event.end_month", "9"),
                    props.getProperty("nap_event.end_hour", "23"),
                    props.getProperty("nap_event.end_min", "59"));
            label.setText("Từ " + start + " đến " + end);
        } catch (Exception ex) {
            label.setText("Không đọc được cấu hình nạp thẻ");
        }
    }

    // Cập nhật label thông tin Top SM
    private void updateSMEventInfoLabel(JLabel label) {
        try {
            java.util.Properties props = new java.util.Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream("data/config/config.properties")) {
                props.load(fis);
            }
            String start = String.format("%s/%s %s:%s",
                    props.getProperty("sm_event.start_day", "12"),
                    props.getProperty("sm_event.start_month", "3"),
                    props.getProperty("sm_event.start_hour", "4"),
                    props.getProperty("sm_event.start_min", "0"));
            String end = String.format("%s/%s %s:%s",
                    props.getProperty("sm_event.end_day", "2"),
                    props.getProperty("sm_event.end_month", "12"),
                    props.getProperty("sm_event.end_hour", "12"),
                    props.getProperty("sm_event.end_min", "12"));
            label.setText("Từ " + start + " đến " + end);
        } catch (Exception ex) {
            label.setText("Không đọc được cấu hình Top SM");
        }
    }

    // Hiển thị danh sách sự kiện đang kích hoạt dựa trên các cờ trong EventManager
    private void updateActiveEventsLabel() {
        try {
            StringBuilder sb = new StringBuilder("Đang chạy: ");
            boolean any = false;
            if (event.EventManager.LUNNAR_NEW_YEAR) { sb.append("Tết, "); any = true; }
            if (event.EventManager.INTERNATIONAL_WOMANS_DAY) { sb.append("QPTG, "); any = true; }
            if (event.EventManager.CHRISTMAS) { sb.append("Noel, "); any = true; }
            if (event.EventManager.HALLOWEEN) { sb.append("Halloween, "); any = true; }
            if (event.EventManager.HUNG_VUONG) { sb.append("Hùng Vương, "); any = true; }
            if (event.EventManager.TRUNG_THU) { sb.append("Trung Thu, "); any = true; }
            if (event.EventManager.TOP_UP) { sb.append("Nạp thẻ, "); any = true; }
            if (!any) {
                activeEventsLabel.setText("Đang chạy: Không có sự kiện");
                return;
            }
            // bỏ dấu phẩy cuối
            String text = sb.toString();
            if (text.endsWith(", ")) text = text.substring(0, text.length()-2);
            activeEventsLabel.setText(text);
        } catch (Exception ex) {
            if (activeEventsLabel != null) activeEventsLabel.setText("Không thể đọc trạng thái sự kiện");
        }
    }
    
    // Panel cấu hình server
    private JPanel createServerConfigPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Cấu hình Server"));
        
        // Load giá trị hiện tại từ config
        String currentExp = loadConfigValue("server.expserver", "3");
        String currentWait = loadConfigValue("server.waitlogin", "5");
        String currentMaxPlayer = loadConfigValue("server.maxplayer", "10000");
        String currentMaxPerIp = loadConfigValue("server.maxperip", "10000");
        
        // Exp Server
        JPanel expPanel = new JPanel(new FlowLayout());
        expPanel.setBorder(BorderFactory.createTitledBorder("Exp Server"));
        JLabel expLabel = new JLabel("Exp:");
        JTextField expField = new JTextField(currentExp, 5);
        JButton expButton = new JButton("Cập nhật");
        expButton.setPreferredSize(new Dimension(80, 30));
        expButton.setFont(new Font("Arial", Font.PLAIN, 10));
        expButton.addActionListener(e -> {
            try {
                int exp = Integer.parseInt(expField.getText());
                if (exp > 0) {
                    updateServerConfig("server.expserver", String.valueOf(exp));
                    JOptionPane.showMessageDialog(this, "Đã cập nhật exp server: " + exp);
                } else {
                    JOptionPane.showMessageDialog(this, "Exp phải lớn hơn 0");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập số hợp lệ");
            }
        });
        expPanel.add(expLabel);
        expPanel.add(expField);
        expPanel.add(expButton);
        
        // Wait Login
        JPanel waitPanel = new JPanel(new FlowLayout());
        waitPanel.setBorder(BorderFactory.createTitledBorder("Wait Login"));
        JLabel waitLabel = new JLabel("Giây:");
        JTextField waitField = new JTextField(currentWait, 5);
        JButton waitButton = new JButton("Cập nhật");
        waitButton.setPreferredSize(new Dimension(80, 30));
        waitButton.setFont(new Font("Arial", Font.PLAIN, 10));
        waitButton.addActionListener(e -> {
            try {
                int wait = Integer.parseInt(waitField.getText());
                if (wait > 0) {
                    updateServerConfig("server.waitlogin", String.valueOf(wait));
                    JOptionPane.showMessageDialog(this, "Đã cập nhật wait login: " + wait + " giây");
                } else {
                    JOptionPane.showMessageDialog(this, "Wait login phải lớn hơn 0");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập số hợp lệ");
            }
        });
        waitPanel.add(waitLabel);
        waitPanel.add(waitField);
        waitPanel.add(waitButton);
        
        // Max Player
        JPanel maxPlayerPanel = new JPanel(new FlowLayout());
        maxPlayerPanel.setBorder(BorderFactory.createTitledBorder("Max Player"));
        JLabel maxPlayerLabel = new JLabel("Số:");
        JTextField maxPlayerField = new JTextField(currentMaxPlayer, 5);
        JButton maxPlayerButton = new JButton("Cập nhật");
        maxPlayerButton.setPreferredSize(new Dimension(80, 30));
        maxPlayerButton.setFont(new Font("Arial", Font.PLAIN, 10));
        maxPlayerButton.addActionListener(e -> {
            try {
                int maxPlayer = Integer.parseInt(maxPlayerField.getText());
                if (maxPlayer > 0) {
                    updateServerConfig("server.maxplayer", String.valueOf(maxPlayer));
                    JOptionPane.showMessageDialog(this, "Đã cập nhật max player: " + maxPlayer);
                } else {
                    JOptionPane.showMessageDialog(this, "Max player phải lớn hơn 0");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập số hợp lệ");
            }
        });
        maxPlayerPanel.add(maxPlayerLabel);
        maxPlayerPanel.add(maxPlayerField);
        maxPlayerPanel.add(maxPlayerButton);
        
        // Max Per IP
        JPanel maxPerIpPanel = new JPanel(new FlowLayout());
        maxPerIpPanel.setBorder(BorderFactory.createTitledBorder("Max Per IP"));
        JLabel maxPerIpLabel = new JLabel("Số:");
        JTextField maxPerIpField = new JTextField(currentMaxPerIp, 5);
        JButton maxPerIpButton = new JButton("Cập nhật");
        maxPerIpButton.setPreferredSize(new Dimension(80, 30));
        maxPerIpButton.setFont(new Font("Arial", Font.PLAIN, 10));
        maxPerIpButton.addActionListener(e -> {
            try {
                int maxPerIp = Integer.parseInt(maxPerIpField.getText());
                if (maxPerIp > 0) {
                    updateServerConfig("server.maxperip", String.valueOf(maxPerIp));
                    JOptionPane.showMessageDialog(this, "Đã cập nhật max per IP: " + maxPerIp);
                } else {
                    JOptionPane.showMessageDialog(this, "Max per IP phải lớn hơn 0");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập số hợp lệ");
            }
        });
        maxPerIpPanel.add(maxPerIpLabel);
        maxPerIpPanel.add(maxPerIpField);
        maxPerIpPanel.add(maxPerIpButton);
        
        panel.add(expPanel);
        panel.add(waitPanel);
        panel.add(maxPlayerPanel);
        panel.add(maxPerIpPanel);
        
        return panel;
    }
    
    // Panel quản lý data versions
    private JPanel createDataVersionPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Data Versions"));
        
        // Load giá trị hiện tại từ config (lấy version đầu tiên làm mẫu)
        String currentVersion = loadConfigValue("data.version.data", "1");
        
        // Panel cập nhật tất cả versions
        JPanel updatePanel = new JPanel(new FlowLayout());
        updatePanel.setBorder(BorderFactory.createTitledBorder("Cập nhật tất cả Data Versions"));
        
        JLabel versionLabel = new JLabel("Version mới:");
        JTextField versionField = new JTextField(currentVersion, 5);
        JButton updateButton = new JButton("Cập nhật tất cả");
        updateButton.setPreferredSize(new Dimension(120, 35));
        updateButton.setFont(new Font("Arial", Font.PLAIN, 11));
        
        updateButton.addActionListener(e -> {
            try {
                int version = Integer.parseInt(versionField.getText());
                if (version > 0) {
                    // Cập nhật tất cả data versions cùng lúc
                    updateAllDataVersions(version);
                    JOptionPane.showMessageDialog(this, 
                        "Đã cập nhật tất cả data versions lên: " + version + "\n" +
                        "- Data Version: " + version + "\n" +
                        "- Map Version: " + version + "\n" +
                        "- Skill Version: " + version + "\n" +
                        "- Item Version: " + version + "\n" +
                        "- Res Version: " + version);
                } else {
                    JOptionPane.showMessageDialog(this, "Version phải lớn hơn 0");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập số hợp lệ");
            }
        });
        
        updatePanel.add(versionLabel);
        updatePanel.add(versionField);
        updatePanel.add(updateButton);
        
        // Panel reload data
        JPanel reloadPanel = new JPanel(new FlowLayout());
        reloadPanel.setBorder(BorderFactory.createTitledBorder("Reload Data"));
        
        JButton reloadButton = new JButton("Reload All Data");
        reloadButton.setPreferredSize(new Dimension(120, 35));
        reloadButton.setFont(new Font("Arial", Font.PLAIN, 11));
        reloadButton.addActionListener(e -> {
            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(this, 
                "Bạn có chắc muốn reload tất cả data versions?\n" +
                "Điều này sẽ khiến client tự động reload data.", 
                "Reload Data", dialogButton);
            if (dialogResult == 0) {
                // Gọi method reload data từ DataGame
                try {
                    Class<?> dataGameClass = Class.forName("data.DataGame");
                    java.lang.reflect.Method reloadMethod = dataGameClass.getMethod("reloadVersions");
                    reloadMethod.invoke(null);
                    JOptionPane.showMessageDialog(this, "Đã reload tất cả data versions!");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Lỗi reload data: " + ex.getMessage());
                }
            }
        });
        
        reloadPanel.add(reloadButton);
        
        panel.add(updatePanel);
        panel.add(reloadPanel);
        
        return panel;
    }
    
    // Method để cập nhật tất cả data versions
    private void updateAllDataVersions(int version) {
        try {
            java.util.Properties props = new java.util.Properties();
            java.io.File configFile = new java.io.File("data/config/config.properties");
            
            if (configFile.exists()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
                    props.load(fis);
                }
            }
            
            // Cập nhật tất cả data versions
            props.setProperty("data.version.data", String.valueOf(version));
            props.setProperty("data.version.map", String.valueOf(version));
            props.setProperty("data.version.skill", String.valueOf(version));
            props.setProperty("data.version.item", String.valueOf(version));
            props.setProperty("data.version.res", String.valueOf(version));
            
            // Tạo nội dung config được sắp xếp theo thứ tự
            StringBuilder configContent = new StringBuilder();
            configContent.append("#SERVER\n");
            configContent.append("server.local=").append(props.getProperty("server.local", "false")).append("\n");
            configContent.append("server.test=").append(props.getProperty("server.test", "false")).append("\n");
            configContent.append("server.daoautoupdater=").append(props.getProperty("server.daoautoupdater", "false")).append("\n");
            configContent.append("server.sv=").append(props.getProperty("server.sv", "1")).append("\n");
            configContent.append("server.name=").append(props.getProperty("server.name", "NRO")).append("\n");
            configContent.append("server.port=").append(props.getProperty("server.port", "1998")).append("\n");
            configContent.append("server.sv1=").append(props.getProperty("server.sv1", "local:127.0.0.1:1998:0,0,0")).append("\n");
            configContent.append("server.waitlogin=").append(props.getProperty("server.waitlogin", "5")).append("\n");
            configContent.append("server.maxperip=").append(props.getProperty("server.maxperip", "10000")).append("\n");
            configContent.append("server.maxplayer=").append(props.getProperty("server.maxplayer", "10000")).append("\n");
            configContent.append("server.expserver=").append(props.getProperty("server.expserver", "3")).append("\n");
            configContent.append("server.debug=").append(props.getProperty("server.debug", "true")).append("\n");
            configContent.append("\n");
            
            configContent.append("#DATABASE\n");
            configContent.append("database.driver=").append(props.getProperty("database.driver", "com.mysql.jdbc.Driver")).append("\n");
            configContent.append("database.host=").append(props.getProperty("database.host", "localhost")).append("\n");
            configContent.append("database.port=").append(props.getProperty("database.port", "3306")).append("\n");
            configContent.append("database.name=").append(props.getProperty("database.name", "nro")).append("\n");
            configContent.append("database.user=").append(props.getProperty("database.user", "root")).append("\n");
            configContent.append("database.pass=").append(props.getProperty("database.pass", "")).append("\n");
            configContent.append("database.min=").append(props.getProperty("database.min", "1")).append("\n");
            configContent.append("database.max=").append(props.getProperty("database.max", "2")).append("\n");
            configContent.append("database.lifetime=").append(props.getProperty("database.lifetime", "120000")).append("\n");
            configContent.append("\n");
            
            configContent.append("#NAP EVENT TIME SETTINGS\n");
            configContent.append("nap_event.start_month=").append(props.getProperty("nap_event.start_month", "3")).append("\n");
            configContent.append("nap_event.start_day=").append(props.getProperty("nap_event.start_day", "15")).append("\n");
            configContent.append("nap_event.start_hour=").append(props.getProperty("nap_event.start_hour", "9")).append("\n");
            configContent.append("nap_event.start_min=").append(props.getProperty("nap_event.start_min", "0")).append("\n");
            configContent.append("nap_event.end_month=").append(props.getProperty("nap_event.end_month", "9")).append("\n");
            configContent.append("nap_event.end_day=").append(props.getProperty("nap_event.end_day", "31")).append("\n");
            configContent.append("nap_event.end_hour=").append(props.getProperty("nap_event.end_hour", "23")).append("\n");
            configContent.append("nap_event.end_min=").append(props.getProperty("nap_event.end_min", "59")).append("\n");
            configContent.append("\n");
            
            configContent.append("#SM EVENT TIME SETTINGS\n");
            configContent.append("sm_event.start_month=").append(props.getProperty("sm_event.start_month", "3")).append("\n");
            configContent.append("sm_event.start_day=").append(props.getProperty("sm_event.start_day", "12")).append("\n");
            configContent.append("sm_event.start_hour=").append(props.getProperty("sm_event.start_hour", "4")).append("\n");
            configContent.append("sm_event.start_min=").append(props.getProperty("sm_event.start_min", "0")).append("\n");
            configContent.append("sm_event.end_month=").append(props.getProperty("sm_event.end_month", "12")).append("\n");
            configContent.append("sm_event.end_day=").append(props.getProperty("sm_event.end_day", "2")).append("\n");
            configContent.append("sm_event.end_hour=").append(props.getProperty("sm_event.end_hour", "12")).append("\n");
            configContent.append("sm_event.end_min=").append(props.getProperty("sm_event.end_min", "12")).append("\n");
            configContent.append("\n");
            
            configContent.append("#DATA RELOAD VERSIONS\n");
            configContent.append("data.version.data=").append(version).append("\n");
            configContent.append("data.version.map=").append(version).append("\n");
            configContent.append("data.version.skill=").append(version).append("\n");
            configContent.append("data.version.item=").append(version).append("\n");
            configContent.append("data.version.res=").append(version).append("\n");
            
            // Ghi file với format đẹp
            try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
                writer.write(configContent.toString());
            }
            
            System.out.println("Đã cập nhật tất cả data versions lên: " + version);
            
        } catch (IOException e) {
            System.out.println("Lỗi cập nhật data versions: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi cập nhật data versions: " + e.getMessage());
        }
    }
    
    // Method để load giá trị từ config
    private String loadConfigValue(String key, String defaultValue) {
        try {
            java.util.Properties props = new java.util.Properties();
            java.io.File configFile = new java.io.File("data/config/config.properties");
            
            if (configFile.exists()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
                    props.load(fis);
                    return props.getProperty(key, defaultValue);
                }
            }
        } catch (IOException e) {
           System.out.println("Lỗi đọc config: " + e.getMessage());
        }
        return defaultValue;
    }
    
    // Method để cập nhật config với format đẹp
    private void updateServerConfig(String key, String value) {
        try {
            java.util.Properties props = new java.util.Properties();
            java.io.File configFile = new java.io.File("data/config/config.properties");
            
            if (configFile.exists()) {
                try (java.io.FileInputStream fis = new java.io.FileInputStream(configFile)) {
                    props.load(fis);
                }
            }
            
            props.setProperty(key, value);
            
            // Tạo nội dung config được sắp xếp theo thứ tự
            StringBuilder configContent = new StringBuilder();
            configContent.append("#SERVER\n");
            configContent.append("server.local=").append(props.getProperty("server.local", "false")).append("\n");
            configContent.append("server.test=").append(props.getProperty("server.test", "false")).append("\n");
            configContent.append("server.daoautoupdater=").append(props.getProperty("server.daoautoupdater", "false")).append("\n");
            configContent.append("server.sv=").append(props.getProperty("server.sv", "1")).append("\n");
            configContent.append("server.name=").append(props.getProperty("server.name", "NRO")).append("\n");
            configContent.append("server.port=").append(props.getProperty("server.port", "1998")).append("\n");
            configContent.append("server.sv1=").append(props.getProperty("server.sv1", "local:127.0.0.1:1998:0,0,0")).append("\n");
            configContent.append("server.waitlogin=").append(props.getProperty("server.waitlogin", "5")).append("\n");
            configContent.append("server.maxperip=").append(props.getProperty("server.maxperip", "10000")).append("\n");
            configContent.append("server.maxplayer=").append(props.getProperty("server.maxplayer", "10000")).append("\n");
            configContent.append("server.expserver=").append(props.getProperty("server.expserver", "3")).append("\n");
            configContent.append("server.debug=").append(props.getProperty("server.debug", "true")).append("\n");
            configContent.append("\n");
            
            configContent.append("#DATABASE\n");
            configContent.append("database.driver=").append(props.getProperty("database.driver", "com.mysql.jdbc.Driver")).append("\n");
            configContent.append("database.host=").append(props.getProperty("database.host", "localhost")).append("\n");
            configContent.append("database.port=").append(props.getProperty("database.port", "3306")).append("\n");
            configContent.append("database.name=").append(props.getProperty("database.name", "nro")).append("\n");
            configContent.append("database.user=").append(props.getProperty("database.user", "root")).append("\n");
            configContent.append("database.pass=").append(props.getProperty("database.pass", "")).append("\n");
            configContent.append("database.min=").append(props.getProperty("database.min", "1")).append("\n");
            configContent.append("database.max=").append(props.getProperty("database.max", "2")).append("\n");
            configContent.append("database.lifetime=").append(props.getProperty("database.lifetime", "120000")).append("\n");
            configContent.append("\n");
            
            configContent.append("#NAP EVENT TIME SETTINGS\n");
            configContent.append("nap_event.start_month=").append(props.getProperty("nap_event.start_month", "3")).append("\n");
            configContent.append("nap_event.start_day=").append(props.getProperty("nap_event.start_day", "15")).append("\n");
            configContent.append("nap_event.start_hour=").append(props.getProperty("nap_event.start_hour", "9")).append("\n");
            configContent.append("nap_event.start_min=").append(props.getProperty("nap_event.start_min", "0")).append("\n");
            configContent.append("nap_event.end_month=").append(props.getProperty("nap_event.end_month", "9")).append("\n");
            configContent.append("nap_event.end_day=").append(props.getProperty("nap_event.end_day", "31")).append("\n");
            configContent.append("nap_event.end_hour=").append(props.getProperty("nap_event.end_hour", "23")).append("\n");
            configContent.append("nap_event.end_min=").append(props.getProperty("nap_event.end_min", "59")).append("\n");
            configContent.append("\n");
            
            configContent.append("#SM EVENT TIME SETTINGS\n");
            configContent.append("sm_event.start_month=").append(props.getProperty("sm_event.start_month", "3")).append("\n");
            configContent.append("sm_event.start_day=").append(props.getProperty("sm_event.start_day", "12")).append("\n");
            configContent.append("sm_event.start_hour=").append(props.getProperty("sm_event.start_hour", "4")).append("\n");
            configContent.append("sm_event.start_min=").append(props.getProperty("sm_event.start_min", "0")).append("\n");
            configContent.append("sm_event.end_month=").append(props.getProperty("sm_event.end_month", "12")).append("\n");
            configContent.append("sm_event.end_day=").append(props.getProperty("sm_event.end_day", "2")).append("\n");
            configContent.append("sm_event.end_hour=").append(props.getProperty("sm_event.end_hour", "12")).append("\n");
            configContent.append("sm_event.end_min=").append(props.getProperty("sm_event.end_min", "12")).append("\n");
            configContent.append("\n");
            
            configContent.append("#DATA RELOAD VERSIONS\n");
            configContent.append("data.version.data=").append(props.getProperty("data.version.data", "1")).append("\n");
            configContent.append("data.version.map=").append(props.getProperty("data.version.map", "1")).append("\n");
            configContent.append("data.version.skill=").append(props.getProperty("data.version.skill", "1")).append("\n");
            configContent.append("data.version.item=").append(props.getProperty("data.version.item", "1")).append("\n");
            configContent.append("data.version.res=").append(props.getProperty("data.version.res", "1")).append("\n");
            
            // Ghi file với format đẹp
            try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
                writer.write(configContent.toString());
            }
            
            System.out.println("Đã cập nhật " + key + " = " + value);
            
        } catch (IOException e) {
            System.out.println("Lỗi cập nhật config: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi cập nhật config: " + e.getMessage());
        }
    }
    
    // Panel thống kê
    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Thông tin Session
        JPanel sessionPanel = new JPanel(new FlowLayout());
        sessionPanel.setBorder(BorderFactory.createTitledBorder("Session"));
        JLabel sessionLabel = new JLabel("Số lượng: 0");
        sessionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        sessionPanel.add(sessionLabel);
        
        // Thông tin Memory
        JPanel memoryPanel = new JPanel(new FlowLayout());
        memoryPanel.setBorder(BorderFactory.createTitledBorder("Memory"));
        JLabel memoryLabel = new JLabel("Tổng: " + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + "MB");
        memoryLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        memoryPanel.add(memoryLabel);
        
        // Thông tin Free Memory
        JPanel freeMemoryPanel = new JPanel(new FlowLayout());
        freeMemoryPanel.setBorder(BorderFactory.createTitledBorder("Free Memory"));
        JLabel freeMemoryLabel = new JLabel("Còn trống: " + (Runtime.getRuntime().freeMemory() / 1024 / 1024) + "MB");
        freeMemoryLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        freeMemoryPanel.add(freeMemoryLabel);
        
        // Thông tin Thread
        JPanel threadPanel = new JPanel(new FlowLayout());
        threadPanel.setBorder(BorderFactory.createTitledBorder("Thread"));
        JLabel threadLabel = new JLabel("Số lượng: " + Thread.activeCount());
        threadLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        threadPanel.add(threadLabel);
        
        panel.add(sessionPanel);
        panel.add(memoryPanel);
        panel.add(freeMemoryPanel);
        panel.add(threadPanel);
        
        // Cập nhật session count real-time (dựa trên EmtiSessionManager)
        Threading.scheduler().scheduleAtFixedRate(() -> {
            int sscount = EmtiSessionManager.gI().getNumSession();
            sessionLabel.setText("Session: " + sscount);
        }, 5, 1, TimeUnit.SECONDS);
        // Cập nhật số thread real-time để đồng bộ với nhãn trên cùng
        Threading.scheduler().scheduleAtFixedRate(() -> {
            threadLabel.setText("Số lượng: " + Thread.activeCount());
        }, 1, 1, TimeUnit.SECONDS);
        
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
            // Chạy trong thread riêng để tránh lag panel
            Threading.runAsync(() -> {
                try {
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
                    
                    // Cập nhật UI trong EDT thread
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(eventDialog, "Đã cập nhật trạng thái sự kiện thành công!");
                        eventDialog.dispose();
                    });
                } catch (Exception ex) {
                    Logger.error("Lỗi cập nhật sự kiện: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(eventDialog, "Lỗi cập nhật sự kiện: " + ex.getMessage());
                    });
                }
            });
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
            // Chạy trong thread riêng để tránh lag panel
            Threading.runAsync(() -> {
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
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(timeDialog, "Vui lòng nhập thời gian hợp lệ!");
                        });
                        return;
                    }
                    
                    // Cập nhật thời gian trong ConstDataEventNAP
                    updateTopUpEventTime(startMonth, startDay, startHour, startMin,
                                       endMonth, endDay, endHour, endMin);
                    
                    Logger.success("Admin đã cập nhật thời gian sự kiện nạp thẻ: " +
                        "Bắt đầu " + startDay + "/" + startMonth + "/2025 " + startHour + ":" + 
                        String.format("%02d", startMin) + " - " +
                        "Kết thúc " + endDay + "/" + endMonth + "/2025 " + endHour + ":" + 
                        String.format("%02d", endMin));
                    
                    // Cập nhật UI trong EDT thread
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(timeDialog, 
                            "Đã cập nhật thời gian sự kiện nạp thẻ thành công!\n" +
                            "Bắt đầu: " + startDay + "/" + startMonth + "/2025 " + startHour + ":" + 
                            String.format("%02d", startMin) + "\n" +
                            "Kết thúc: " + endDay + "/" + endMonth + "/2025 " + endHour + ":" + 
                            String.format("%02d", endMin));
                        timeDialog.dispose();
                    });
                    
                } catch (NumberFormatException ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(timeDialog, "Vui lòng nhập số hợp lệ!");
                    });
                } catch (Exception ex) {
                    Logger.error("Lỗi cập nhật thời gian sự kiện nạp thẻ: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(timeDialog, "Lỗi cập nhật: " + ex.getMessage());
                    });
                }
            });
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
            // Chạy trong thread riêng để tránh lag panel
            Threading.runAsync(() -> {
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
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(timeDialog, "Vui lòng nhập thời gian hợp lệ!");
                        });
                        return;
                    }
                    
                    // Cập nhật thời gian trong ConstDataEventSM
                    updateTopSMEventTime(startMonth, startDay, startHour, startMin,
                                       endMonth, endDay, endHour, endMin);
                    
                    Logger.success("Admin đã cập nhật thời gian sự kiện top sức mạnh: " +
                        "Bắt đầu " + startDay + "/" + startMonth + "/2025 " + startHour + ":" + 
                        String.format("%02d", startMin) + " - " +
                        "Kết thúc " + endDay + "/" + endMonth + "/2025 " + endHour + ":" + 
                        String.format("%02d", endMin));
                    
                    // Cập nhật UI trong EDT thread
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(timeDialog, 
                            "Đã cập nhật thời gian sự kiện top sức mạnh thành công!\n" +
                            "Bắt đầu: " + startDay + "/" + startMonth + "/2025 " + startHour + ":" + 
                            String.format("%02d", startMin) + "\n" +
                            "Kết thúc: " + endDay + "/" + endMonth + "/2025 " + endHour + ":" + 
                            String.format("%02d", endMin));
                        timeDialog.dispose();
                    });
                    
                } catch (NumberFormatException ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(timeDialog, "Vui lòng nhập số hợp lệ!");
                    });
                } catch (Exception ex) {
                    Logger.error("Lỗi cập nhật thời gian sự kiện top sức mạnh: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(timeDialog, "Lỗi cập nhật: " + ex.getMessage());
                    });
                }
            });
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
    private void scheduleMaintenance(JComboBox<Integer> hoursComboBox, JComboBox<Integer> minutesComboBox, JComboBox<Integer> secondsComboBox, boolean showPopup) {
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
        if (showPopup) {
            String hh = hours < 10 ? ("0" + hours) : String.valueOf(hours);
            String mm = minutes < 10 ? ("0" + minutes) : String.valueOf(minutes);
            String ss = seconds < 10 ? ("0" + seconds) : String.valueOf(seconds);
            JOptionPane.showMessageDialog(this, "Đã cài đặt quá trình bảo trì tự động vào lúc " + hh + ":" + mm + ":" + ss + " (bảo trì 15 giây)");
        }
        
        Threading.scheduler().scheduleAtFixedRate(() -> {
            if (timeReached.get()) return;
            try {
                LocalTime currentTime = LocalTime.now();
                int hourss = hoursComboBox.getItemAt(hoursComboBox.getSelectedIndex());
                int minutess = minutesComboBox.getItemAt(minutesComboBox.getSelectedIndex());
                int secondss = secondsComboBox.getItemAt(secondsComboBox.getSelectedIndex());
                int hour_now = currentTime.getHour();
                int minute_now = currentTime.getMinute();
                int seconds_now = currentTime.getSecond();

                if (hourss == hour_now && minutess == minute_now && secondss == seconds_now) {
                    Threading.runAsync(() -> {
                        try {
                            performMaintenance();
                        } catch (Exception ex) {
                            Logger.error("Lỗi thực hiện bảo trì: " + ex.getMessage());
                        }
                    });
                    timeReached.set(true);
                }
            } catch (Exception e) {
                Logger.error("Lỗi trong scheduler hẹn giờ bảo trì: " + e.getMessage());
            }
        }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);
    }
    
    private void performMaintenance() {
        Maintenance.gI().start(15); // 15 giây
    }
}
