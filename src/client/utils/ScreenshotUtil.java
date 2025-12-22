package client.utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

/**
 * 截图工具类 - 提供屏幕截图功能
 * <p>
 * 【核心作用】
 * 1. 全屏截图（captureFullScreen）
 * 2. 区域选择截图（captureArea）
 * 3. 截图保存和格式转换
 * <p>
 * 【使用场景】
 * 用户在聊天界面点击"截图"按钮时调用，
 * 支持框选屏幕区域后发送给聊天对象
 * <p>
 * 【快捷键】
 * - ESC：取消截图
 * - 鼠标拖拽：选择截图区域
 * 
 * @author ChatRoom Team
 */
public class ScreenshotUtil {

    /**
     * 截取全屏
     */
    public static BufferedImage captureFullScreen() throws AWTException {
        Robot robot = new Robot();
        Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        return robot.createScreenCapture(screenRect);
    }

    /**
     * 截取指定区域（简化版：使用对话框让用户选择区域）
     */
    public static BufferedImage captureArea() {
        try {
            // 隐藏所有窗口
            Robot robot = new Robot();
            Thread.sleep(200); // 等待窗口隐藏

            BufferedImage fullScreen = captureFullScreen();

            // 创建选择框
            JFrame frame = new JFrame();
            frame.setUndecorated(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setAlwaysOnTop(true);

            final BufferedImage[] result = { null };
            final Point[] startPoint = { null };
            final Point[] endPoint = { null };

            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.drawImage(fullScreen, 0, 0, null);

                    // 绘制半透明遮罩
                    g.setColor(new Color(0, 0, 0, 100));
                    g.fillRect(0, 0, getWidth(), getHeight());

                    // 绘制选择框
                    if (startPoint[0] != null && endPoint[0] != null) {
                        int x = Math.min(startPoint[0].x, endPoint[0].x);
                        int y = Math.min(startPoint[0].y, endPoint[0].y);
                        int w = Math.abs(endPoint[0].x - startPoint[0].x);
                        int h = Math.abs(endPoint[0].y - startPoint[0].y);

                        // 清除选择区域的遮罩
                        g.setColor(Color.WHITE);
                        g.drawRect(x, y, w, h);

                        // 显示尺寸
                        g.setColor(Color.RED);
                        g.drawString(w + " x " + h, x, y - 5);
                    }
                }
            };

            panel.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    startPoint[0] = e.getPoint();
                }

                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    endPoint[0] = e.getPoint();

                    if (startPoint[0] != null && endPoint[0] != null) {
                        int x = Math.min(startPoint[0].x, endPoint[0].x);
                        int y = Math.min(startPoint[0].y, endPoint[0].y);
                        int w = Math.abs(endPoint[0].x - startPoint[0].x);
                        int h = Math.abs(endPoint[0].y - startPoint[0].y);

                        if (w > 0 && h > 0) {
                            result[0] = fullScreen.getSubimage(x, y, w, h);
                        }
                    }
                    frame.dispose();
                }
            });

            panel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseDragged(java.awt.event.MouseEvent e) {
                    endPoint[0] = e.getPoint();
                    panel.repaint();
                }
            });

            // ESC取消
            panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke("ESCAPE"), "cancel");
            panel.getActionMap().put("cancel", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    frame.dispose();
                }
            });

            frame.add(panel);
            frame.setVisible(true);

            // 等待截图完成
            while (frame.isVisible()) {
                Thread.sleep(100);
            }

            return result[0];

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 保存图片到文件
     */
    public static File saveImage(BufferedImage image) throws Exception {
        File file = new File("screenshots/screenshot_" + System.currentTimeMillis() + ".png");
        file.getParentFile().mkdirs();
        ImageIO.write(image, "PNG", file);
        return file;
    }

    /**
     * 图片转字节数组
     */
    public static byte[] imageToBytes(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}
