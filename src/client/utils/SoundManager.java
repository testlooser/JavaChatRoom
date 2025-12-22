package client.utils;

import javax.sound.sampled.*;
import java.io.File;

/**
 * 声音管理器 - 播放系统音效
 * <p>
 * 【核心作用】
 * 1. 播放消息提示音（新消息到达时）
 * 2. 播放用户上线提示音
 * 3. 播放文件接收提示音
 * <p>
 * 【音效文件路径】
 * resources/sounds/目录下：
 * - message.wav：新消息提示音
 * - online.wav：用户上线提示音
 * - file.wav：文件接收提示音
 * <p>
 * 【降级策略】
 * 如果音效文件不存在，自动使用系统beep作为替代
 * 
 * @author ChatRoom Team
 */
public class SoundManager {

    /**
     * 播放声音文件
     */
    public static void playSound(String soundFilePath) {
        new Thread(() -> {
            try {
                File soundFile = new File(soundFilePath);
                if (!soundFile.exists()) {
                    // 如果文件不存在，使用系统beep
                    java.awt.Toolkit.getDefaultToolkit().beep();
                    return;
                }

                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();

                // 等待播放完成
                Thread.sleep(clip.getMicrosecondLength() / 1000);
                clip.close();
                audioStream.close();
            } catch (Exception e) {
                // 播放失败，使用系统beep
                java.awt.Toolkit.getDefaultToolkit().beep();
            }
        }).start();
    }

    /**
     * 播放新消息提示音
     */
    public static void playMessageSound() {
        playSound("resources/sounds/message.wav");
    }

    /**
     * 播放用户上线提示音
     */
    public static void playOnlineSound() {
        playSound("resources/sounds/online.wav");
    }

    /**
     * 播放文件接收提示音
     */
    public static void playFileSound() {
        playSound("resources/sounds/file.wav");
    }

    /**
     * 简单beep
     */
    public static void beep() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }
}
