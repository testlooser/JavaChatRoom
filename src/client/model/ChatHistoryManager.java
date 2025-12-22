package client.model;

import common.Message;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天记录管理器 - 负责聊天记录的持久化存储和加载
 * <p>
 * 【核心作用】
 * 1. 将聊天记录序列化保存到本地文件
 * 2. 从文件加载历史聊天记录
 * 3. 提供内存缓存加速读取性能
 * <p>
 * 【存储结构】
 * - 基础目录：chathistory/{UserID}/
 * - 文件格式：chat_{聊天标识}.ser（Java序列化格式）
 * - 聊天标识：用户ID、"世界聊天"、"群聊:成员1,成员2,..."
 * <p>
 * 【缓存策略】
 * - 使用ConcurrentHashMap作为内存缓存
 * - 读取时先查缓存，未命中再从文件加载
 * - 保存时同时更新缓存和文件
 * 
 * @author ChatRoom Team
 */
public class ChatHistoryManager {

    private static final String STORAGE_BASE_DIR = "chathistory";
    private final String userStorageDir;
    private final ConcurrentHashMap<String, List<Message>> cache;

    /**
     * 构造函数
     * 
     * @param userId 当前登录用户ID
     */
    public ChatHistoryManager(String userId) {
        this.userStorageDir = STORAGE_BASE_DIR + File.separator + userId;
        this.cache = new ConcurrentHashMap<>();

        // 创建存储目录
        File dir = new File(userStorageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * 保存聊天记录到文件
     * 
     * @param chatKey  聊天标识（用户ID、"世界聊天"、"群聊:..."）
     * @param messages 消息列表
     */
    public void saveMessages(String chatKey, List<Message> messages) {
        if (chatKey == null || messages == null) {
            return;
        }

        // 更新缓存
        cache.put(chatKey, new ArrayList<>(messages));

        // 保存到文件
        String filename = getFilename(chatKey);
        File file = new File(userStorageDir, filename);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(new ArrayList<>(messages));
            System.out.println("聊天记录已保存: " + filename + " (" + messages.size() + "条)");
        } catch (IOException e) {
            System.err.println("保存聊天记录失败: " + filename);
            e.printStackTrace();
        }
    }

    /**
     * 从文件加载聊天记录
     * 
     * @param chatKey 聊天标识
     * @return 消息列表，如果文件不存在或读取失败返回空列表
     */
    @SuppressWarnings("unchecked")
    public List<Message> loadMessages(String chatKey) {
        if (chatKey == null) {
            return new ArrayList<>();
        }

        // 先检查缓存
        if (cache.containsKey(chatKey)) {
            return new ArrayList<>(cache.get(chatKey));
        }

        // 从文件加载
        String filename = getFilename(chatKey);
        File file = new File(userStorageDir, filename);

        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<Message> messages = (List<Message>) ois.readObject();
            cache.put(chatKey, messages);
            System.out.println("聊天记录已加载: " + filename + " (" + messages.size() + "条)");
            return new ArrayList<>(messages);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("加载聊天记录失败: " + filename);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 添加单条消息并保存
     * 
     * @param chatKey 聊天标识
     * @param message 消息
     */
    public void addMessage(String chatKey, Message message) {
        List<Message> messages = loadMessages(chatKey);
        messages.add(message);
        saveMessages(chatKey, messages);
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * 删除指定聊天的记录文件
     * 
     * @param chatKey 聊天标识
     */
    public void deleteHistory(String chatKey) {
        String filename = getFilename(chatKey);
        File file = new File(userStorageDir, filename);
        if (file.exists()) {
            file.delete();
            cache.remove(chatKey);
        }
    }

    /**
     * 获取存储目录路径
     * 
     * @return 存储目录绝对路径
     */
    public String getStorageDirectory() {
        return new File(userStorageDir).getAbsolutePath();
    }

    /**
     * 生成文件名
     * 
     * @param chatKey 聊天标识
     * @return 文件名
     */
    private String getFilename(String chatKey) {
        // 清理文件名中的非法字符
        String sanitized = chatKey.replaceAll("[\\\\/:*?\"<>|]", "_");
        return "chat_" + sanitized + ".ser";
    }
}
