package server.service;

import common.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端线程管理器 - 管理所有在线用户的通信线程
 * <p>
 * 【核心作用】
 * 1. 维护在线用户的线程映射表（UserID -> 通信线程）
 * 2. 管理离线消息缓存（用户离线时暂存消息）
 * 3. 提供线程的添加、查询、移除方法
 * <p>
 * 【线程安全】
 * 使用ConcurrentHashMap保证多线程环境下的安全访问
 * <p>
 * 【离线消息机制】
 * 当目标用户不在线时，消息存入offlineMessages
 * 用户重新上线时，由ServerConnectClientThread推送缓存消息
 * 
 * @author ChatRoom Team
 */
public class ManageClientThread {

	/**
	 * 在线用户线程映射表
	 * key: UserID（8位数字）
	 * value: 该用户对应的通信线程
	 */
	private static final ConcurrentHashMap<String, ServerConnectClientThread> clientThreads = new ConcurrentHashMap<>();
	
	/**
	 * 离线消息缓存
	 * key: 接收者UserID
	 * value: 待发送的消息列表
	 */
	public static Map<String, List<Message>> offlineMessages = new ConcurrentHashMap<>();

	public static void addClientThread(String userId, ServerConnectClientThread thread) {
		clientThreads.put(userId, thread);
	}

	public static ServerConnectClientThread getClientThread(String userId) {
		return clientThreads.get(userId);
	}

	public static void removeClientThread(String userId) {
		clientThreads.remove(userId);
	}

	public static ConcurrentHashMap<String, ServerConnectClientThread> getAll() {
		return clientThreads;
	}

	public static List<String> getOnlineUsers() {
		return new ArrayList<>(clientThreads.keySet());
	}
}
