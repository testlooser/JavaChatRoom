package client.service;

import client.model.ChatHistoryManager;
import common.Message;
import common.MessageType;
import common.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端业务服务类 - 处理用户操作和消息管理
 * <p>
 * 【核心作用】
 * 1. 封装发送消息的业务逻辑
 * 2. 管理消息缓存（内存+文件持久化）
 * 3. 提供请求在线用户列表等功能
 * <p>
 * 【设计说明】
 * - 采用单例模式，全局唯一实例
 * - 不负责接收消息（由ClientConnectServer线程处理）
 * - 消息缓存采用ConcurrentHashMap保证线程安全
 * <p>
 * 【消息缓存机制】
 * - 内存缓存：messageBuffer (ConcurrentHashMap)
 * - 文件持久化：ChatHistoryManager
 * - 读取时先查内存，未命中则从文件加载
 * 
 * @author ChatRoom Team
 */
public class ManageClientService {
	/** 单例实例 */
	private static ManageClientService instance;

	/** 当前登录用户 */
	private final User user;

	/** 聊天历史文件管理器 */
	private final ChatHistoryManager historyManager;

	/**
	 * 消息缓存区
	 * <p>
	 * key: 聊天对象标识（UserID / "世界聊天" / "群聊:成员1,成员2,..."）
	 * value: 该聊天的消息列表
	 * <p>
	 * 【线程安全】使用ConcurrentHashMap保证多线程操作安全
	 */
	private final ConcurrentHashMap<String, List<Message>> messageBuffer = new ConcurrentHashMap<>();

	/**
	 * 私有构造器 - 初始化服务
	 * 
	 * @param user 当前登录用户
	 */
	private ManageClientService(User user) {
		this.user = user;
		this.historyManager = new ChatHistoryManager(user.getUserID());
		System.out.println("聊天记录存储目录: " + historyManager.getStorageDirectory());
	}

	/**
	 * 获取单例实例（需要初始化参数）
	 * 【首次调用】创建实例并初始化聊天历史管理器
	 * 
	 * @param user 当前用户
	 * @return 单例实例
	 */
	public static synchronized ManageClientService getInstance(User user) throws IOException {
		if (instance == null) {
			instance = new ManageClientService(user);
		}
		return instance;
	}

	/**
	 * 获取单例实例（无参数版本）
	 * 【前置条件】必须先调用带参数的getInstance初始化
	 */
	public static synchronized ManageClientService getInstance() {
		if (instance == null) {
			System.out.println("instance is null");
			return null;
		}
		return instance;
	}

	/**
	 * 请求获取在线用户列表
	 * 【作用】发送请求后，服务器会返回在线用户列表
	 */
	public void requestOnlineUsers() {
		Message message = new Message.Builder()
				.mesType(MessageType.MESSAGE_GET_ONLINE_FRIEND)
				.sender(user.getName())
				.build();
		ClientConnectServer.getInstance().sendMessage(message);
	}

	/**
	 * 发送消息到服务器
	 * 【通用方法】支持私聊、群聊、世界聊天等所有类型的消息
	 * 
	 * @param message 要发送的消息
	 */
	public void sendPrivateMessage(Message message) {
		ClientConnectServer.getInstance().sendMessage(message);
	}

	/**
	 * 发送退出请求
	 * 【作用】通知服务器用户下线，服务器会广播更新在线列表
	 */
	public void sendExitRequest() {
		Message message = new Message.Builder()
				.mesType(MessageType.MESSAGE_CLIENT_EXIT)
				.build();
		ClientConnectServer.getInstance().sendMessage(message);
		System.out.println("已发送退出请求，程序即将结束。");
	}

	/**
	 * 将消息添加到缓存区并自动保存到文件
	 * <p>
	 * 【关键方法】接收消息时调用，实现消息持久化
	 * 
	 * @param key 聊天对象标识（用户ID或"世界聊天"或"群聊:..."）
	 * @param msg 消息对象
	 */
	public void addMessageToBuffer(String key, Message msg) {
		messageBuffer.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(msg);
		// 自动保存到文件，实现持久化
		historyManager.saveMessages(key, messageBuffer.get(key));
	}

	/**
	 * 获取指定聊天对象的所有消息
	 * <p>
	 * 【缓存策略】
	 * 1. 先查内存缓存
	 * 2. 未命中则从文件加载并放入缓存
	 * 
	 * @param key 聊天对象标识
	 * @return 消息列表（如果不存在则返回空列表）
	 */
	public List<Message> getMessages(String key) {
		// 如果内存中没有，尝试从文件加载
		if (!messageBuffer.containsKey(key)) {
			List<Message> loadedMessages = historyManager.loadMessages(key);
			if (!loadedMessages.isEmpty()) {
				messageBuffer.put(key, Collections.synchronizedList(loadedMessages));
			}
		}
		return messageBuffer.getOrDefault(key, Collections.emptyList());
	}

	/**
	 * 清除指定聊天对象的消息缓存
	 * 【注意】仅清除内存缓存，不删除文件
	 * 
	 * @param key 聊天对象标识
	 */
	public void clearMessages(String key) {
		messageBuffer.remove(key);
	}
}
