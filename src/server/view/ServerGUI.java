package server.view;

import common.User;
import server.db.UserDao;
import server.service.ManageClientThread;
import server.service.ServerConnectClientThread;
import common.Message;
import common.MessageType;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 服务器GUI管理界面
 * 包含：用户列表、消息日志、系统广播
 */
public class ServerGUI extends JFrame {

    /** 用户列表组件 */
    private final JList<String> userListComponent;
    /** 用户列表数据模型 */
    private final DefaultListModel<String> userListModel;
    /** 用户详情显示区域 */
    private final JTextArea userDetailArea;

    // 第二部分：日志（右上）
    private final JTextArea logArea;

    // 第三部分：系统广播（右下）
    private final JTextField broadcastInput;
    private final JButton broadcastButton;

    public ServerGUI() {
        // 设置UTF-8编码

        setTitle("服务器监控面板");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ===== 左侧：用户列表面板 (30%) =====
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(new TitledBorder("在线用户列表"));
        leftPanel.setPreferredSize(new Dimension(300, getHeight()));

        userListModel = new DefaultListModel<>();
        userListComponent = new JList<>(userListModel);
        userListComponent.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        JScrollPane userListScroll = new JScrollPane(userListComponent);

        userDetailArea = new JTextArea();
        userDetailArea.setEditable(false);
        userDetailArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        userDetailArea.setBorder(new TitledBorder("用户详细信息"));
        JScrollPane detailScroll = new JScrollPane(userDetailArea);
        detailScroll.setPreferredSize(new Dimension(300, 150));

        leftPanel.add(userListScroll, BorderLayout.CENTER);
        leftPanel.add(detailScroll, BorderLayout.SOUTH);

        // 用户列表选择监听器
        userListComponent.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = userListComponent.getSelectedValue();
                if (selected != null) {
                    String userId = extractUserId(selected);
                    showUserDetail(userId);
                }
            }
        });

        // ===== 右侧：日志和广播面板 =====
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));

        // 右上：日志面板 (60%)
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new TitledBorder("消息日志"));

        logArea = new JTextArea();
        logArea.setEditable(false);
        // 使用 "Microsoft YaHei" 既支持中文也支持英文
        logArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        logArea.setLineWrap(true);
        JScrollPane logScroll = new JScrollPane(logArea);
        logPanel.add(logScroll, BorderLayout.CENTER);

        // 右下：系统广播面板 (40%)
        JPanel broadcastPanel = new JPanel(new BorderLayout(5, 5));
        broadcastPanel.setBorder(new TitledBorder("系统广播"));
        broadcastPanel.setPreferredSize(new Dimension(getWidth(), 200));

        JLabel broadcastLabel = new JLabel("系统消息:");
        broadcastInput = new JTextField();
        broadcastInput.setFont(new Font("微软雅黑", Font.PLAIN, 14));

        broadcastButton = new JButton("发送广播");
        broadcastButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        broadcastButton.setPreferredSize(new Dimension(120, 30));

        JPanel broadcastInputPanel = new JPanel(new BorderLayout(5, 5));
        broadcastInputPanel.add(broadcastLabel, BorderLayout.WEST);
        broadcastInputPanel.add(broadcastInput, BorderLayout.CENTER);
        broadcastInputPanel.add(broadcastButton, BorderLayout.EAST);

        broadcastPanel.add(broadcastInputPanel, BorderLayout.NORTH);

        // 添加发送广播功能
        broadcastButton.addActionListener(e -> broadcastSystemMessage());
        broadcastInput.addActionListener(e -> broadcastSystemMessage());

        // 组装右侧面板
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, logPanel, broadcastPanel);
        rightSplitPane.setResizeWeight(0.6);
        rightSplitPane.setDividerLocation(350);
        rightPanel.add(rightSplitPane, BorderLayout.CENTER);

        // 组装主面板
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        mainSplitPane.setResizeWeight(0.3);
        mainSplitPane.setDividerLocation(300);
        mainPanel.add(mainSplitPane, BorderLayout.CENTER);

        add(mainPanel);

        // 初始日志
        appendLog("服务器启动成功，监听端口 8888");
        appendLog("等待客户端连接...");
    }

    /**
     * 更新在线用户列表
     */
    public void updateUserList(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : users) {
                userListModel.addElement(user + " [在线]");
            }
            if (userListModel.isEmpty()) {
                userListModel.addElement("暂无在线用户");
            }
        });
    }

    /**
     * 显示用户详细信息
     */
    private void showUserDetail(String userId) {
        try {
            UserDao dao = new UserDao();
            User user = dao.getUserById(userId);
            dao.closeConnection();

            if (user != null) {
                userDetailArea.setText(
                        "账号: " + user.getUserID() + "\n" +
                                "昵称: " + user.getNicname() + "\n" +
                                "性别: " + user.getGender() + "\n" +
                                "头像: " + user.getAvatar() + "\n");
            }
        } catch (SQLException e) {
            userDetailArea.setText("用户信息查询失败");
            e.printStackTrace();
        }
    }

    /**
     * 添加日志
     */
    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String timestamp = sdf.format(new Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * 发送系统广播
     */
    private void broadcastSystemMessage() {
        String content = broadcastInput.getText().trim();
        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入广播内容", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Message sysMsg = new Message.Builder()
                .mesType(MessageType.MESSAGE_SYSTEM_BROADCAST)
                .sender("系统管理员")
                .content(content)
                .build();

        // 发送给所有在线用户
        int count = 0;
        for (ServerConnectClientThread thread : ManageClientThread.getAll().values()) {
            try {
                thread.getOOS().writeObject(sysMsg);
                count++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        appendLog("【系统广播】发送给 " + count + " 个用户: \"" + content + "\"");
        broadcastInput.setText("");
    }

    /**
     * 从显示名称中提取UserID
     */
    private String extractUserId(String displayName) {
        if (displayName == null)
            return "";
        int startIdx = displayName.lastIndexOf('(');
        int endIdx = displayName.lastIndexOf(')');
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            return displayName.substring(startIdx + 1, endIdx);
        }
        return displayName;
    }
}
