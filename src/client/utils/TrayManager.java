package client.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 系统托盘管理器 - 管理应用的系统托盘图标
 * <p>
 * 【核心作用】
 * 1. 最小化到系统托盘
 * 2. 托盘图标闪烁（有新消息时）
 * 3. 托盘右键菜单
 * <p>
 * 【托盘功能】
 * - 双击托盘图标：恢复窗口
 * - 右键菜单：显示/退出
 * - 图标闪烁：提示有新消息
 * <p>
 * 【兼容性】
 * 需要操作系统支持SystemTray，否则功能降级
 * 
 * @author ChatRoom Team
 */
public class TrayManager {
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    private JFrame mainFrame;
    private Timer flashTimer;
    private boolean isFlashing = false;

    // 两个图标用于闪动效果（可以用同一个图标，通过显示/隐藏实现）
    private Image normalIcon;
    private Image emptyIcon;

    public TrayManager(JFrame frame) {
        this.mainFrame = frame;

        if (!SystemTray.isSupported()) {
            System.out.println("系统托盘不支持");
            return;
        }

        try {
            systemTray = SystemTray.getSystemTray();

            // 创建托盘图标（使用简单的内存图标）
            normalIcon = createColorIcon(Color.GREEN, 16);
            emptyIcon = createColorIcon(Color.WHITE, 16);

            trayIcon = new TrayIcon(normalIcon, "聊天客户端");
            trayIcon.setImageAutoSize(true);

            // 双击恢复窗口
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        restoreWindow();
                    }
                }
            });

            // 右键菜单
            PopupMenu popup = new PopupMenu();
            MenuItem showItem = new MenuItem("显示");
            MenuItem exitItem = new MenuItem("退出");

            showItem.addActionListener(e -> restoreWindow());
            exitItem.addActionListener(e -> System.exit(0));

            popup.add(showItem);
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 最小化到托盘
     */
    public void minimizeToTray() {
        try {
            if (trayIcon != null && systemTray != null) {
                systemTray.add(trayIcon);
                mainFrame.setVisible(false);
            }
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    /**
     * 恢复窗口
     */
    public void restoreWindow() {
        if (mainFrame != null) {
            mainFrame.setVisible(true);
            mainFrame.setExtendedState(JFrame.NORMAL);
            mainFrame.toFront();
            stopFlashing();
            if (trayIcon != null && systemTray != null) {
                systemTray.remove(trayIcon);
            }
        }
    }

    /**
     * 开始闪动托盘图标
     */
    public void startFlashing() {
        if (isFlashing || trayIcon == null)
            return;

        isFlashing = true;
        flashTimer = new Timer(500, e -> {
            // 切换图标
            if (trayIcon.getImage() == normalIcon) {
                trayIcon.setImage(emptyIcon);
            } else {
                trayIcon.setImage(normalIcon);
            }
        });
        flashTimer.start();
    }

    /**
     * 停止闪动
     */
    public void stopFlashing() {
        if (flashTimer != null) {
            flashTimer.stop();
            isFlashing = false;
            if (trayIcon != null) {
                trayIcon.setImage(normalIcon);
            }
        }
    }

    /**
     * 创建简单的颜色图标
     */
    private Image createColorIcon(Color color, int size) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(color);
        g2.fillOval(0, 0, size, size);
        g2.dispose();
        return img;
    }

    /**
     * 显示托盘消息
     */
    public void showMessage(String caption, String text) {
        if (trayIcon != null) {
            trayIcon.displayMessage(caption, text, TrayIcon.MessageType.INFO);
        }
    }
}
