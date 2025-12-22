package client.service;

import common.Message;
import common.MessageType;
import common.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端连接服务器线程类 - 负责与服务器的网络通信
 * <p>
 * 【核心作用】
 * 1. 管理客户端与服务器的Socket连接
 * 2. 发送登录请求并处理响应
 * 3. 后台线程持续接收服务器消息
 * 4. 通过监听器机制分发消息给UI层
 * <p>
 * 【设计模式】
 * - 单例模式：全局唯一的服务器连接实例
 * - 观察者模式：通过Listener接口通知UI更新
 * <p>
 * 【线程安全】
 * - 继承Thread类，run()方法在独立线程中执行
 * - 持续监听服务器消息，不阻塞UI线程
 * 
 * @author ChatRoom Team
 */
public class ClientConnectServer extends Thread {

	/** 单例实例 */
	private static ClientConnectServer instance;

	/** 当前登录用户 */
	private final User user;

	/** 与服务器的Socket连接 */
	private final Socket socket;

	/** 对象输入流 - 接收服务器消息 */
	private ObjectInputStream ois;

	/** 对象输出流 - 发送消息到服务器 */
	private ObjectOutputStream oos;

	/** 线程运行状态标志 */
	private boolean isRunning = true;

	// ==================== 监听器列表 ====================
	/** 在线用户列表更新监听器 */
	private final List<OnlineUserListener> onlineUserListeners = new ArrayList<>();
	/** 私聊/群聊消息监听器 */
	private final List<PrivateMessageListener> privateMessageListeners = new ArrayList<>();

	/**
	 * 在线用户列表监听器接口
	 * 【作用】当收到在线用户列表更新时通知UI刷新
	 */
	public interface OnlineUserListener {
		void onMessageReceived(Message msg);
	}

	/** 添加在线用户监听器 */
	public void addOnlineUserListener(OnlineUserListener listener) {
		if (listener != null)
			onlineUserListeners.add(listener);
	}

	/** 移除在线用户监听器 */
	public void removeOnlineUserListener(OnlineUserListener listener) {
		onlineUserListeners.remove(listener);
	}

	/**
	 * 私聊消息监听器接口
	 * 【作用】当收到私聊/群聊/世界聊天消息时通知UI显示
	 */
	public interface PrivateMessageListener {
		void onMessageReceived(Message msg);
	}

	/** 添加私聊消息监听器 */
	public void addPrivateMessageListener(PrivateMessageListener listener) {
		if (listener != null)
			privateMessageListeners.add(listener);
	}

	/** 移除私聊消息监听器 */
	public void removePrivateMessageListener(PrivateMessageListener listener) {
		privateMessageListeners.remove(listener);
	}

	/** 群聊创建监听器列表 */
	private final List<GroupCreateListener> groupCreateListeners = new ArrayList<>();

	/**
	 * 群聊创建监听器接口
	 * 【作用】当收到群聊创建通知时，更新本地群聊列表
	 */
	public interface GroupCreateListener {
		void onGroupCreated(Message msg);
	}

	/** 添加群聊创建监听器 */
	public void addGroupCreateListener(GroupCreateListener listener) {
		if (listener != null)
			groupCreateListeners.add(listener);
	}

	/**
	 * 私有构造器 - 初始化Socket连接和IO流
	 * 【设计说明】配合单例模式，只能通过getInstance获取实例
	 */
	private ClientConnectServer(User user, Socket socket) throws IOException {
		this.user = user;
		this.socket = socket;
		oos = new ObjectOutputStream(socket.getOutputStream());
		ois = new ObjectInputStream(socket.getInputStream());
	}

	/**
	 * 获取单例实例（需要初始化参数）
	 * 【关键方法】首次调用时创建实例，后续调用返回已有实例
	 * 
	 * @param user   当前用户
	 * @param socket 服务器连接
	 * @return 单例实例
	 */
	public static synchronized ClientConnectServer getInstance(User user, Socket socket) throws IOException {
		if (instance == null) {
			instance = new ClientConnectServer(user, socket);
		}
		return instance;
	}

	/**
	 * 获取单例实例（无参数版本）
	 * 【前置条件】必须先调用带参数的getInstance初始化
	 */
	public static synchronized ClientConnectServer getInstance() {
		return instance;
	}

	/**
	 * 发送登录请求到服务器
	 * <p>
	 * 【关键方法】
	 * 1. 构建登录消息发送到服务器
	 * 2. 等待服务器响应
	 * 3. 登录成功后启动消息接收线程
	 * 4. 从响应中获取昵称并更新用户信息
	 * 
	 * @return true登录成功，false登录失败
	 */
	public boolean sendLoginRequest() throws IOException, ClassNotFoundException {
		Message loginMsg = new Message.Builder()
				.mesType(MessageType.MESSAGE_LOGIN)
				.sender(user.getUserID())
				.content(user.getPassword())
				.build();
		sendMessage(loginMsg);
		Message response = (Message) ois.readObject();
		if (MessageType.MESSAGE_LOGIN_SUCCEED.equals(response.getMesType())) {
			// 从服务器返回的消息中获取昵称并更新
			String nickname = response.getContent();
			if (nickname != null && !nickname.isEmpty()) {
				user.setName(nickname);
				user.setNicname(nickname);
			}
			this.start(); // 启动消息接收线程
			return true;
		} else {
			closeConnection();
			return false;
		}
	}

	/**
	 * 消息接收线程主循环
	 * <p>
	 * 【核心方法】后台持续运行，接收并处理服务器消息
	 * 【线程安全】在独立线程中运行，不阻塞UI
	 */
	@Override
	public void run() {
		while (isRunning) {
			try {
				Message msg = (Message) ois.readObject();
				handleMessage(msg);
			} catch (IOException | ClassNotFoundException e) {
				isRunning = false;
				closeConnection();
			}
		}
	}

	/**
	 * 发送消息到服务器
	 * 
	 * @param message 要发送的消息对象
	 */
	public void sendMessage(Message message) {
		try {
			oos.writeObject(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 关闭连接并释放资源
	 * 【重要】关闭时重置单例instance为null
	 */
	public void closeConnection() {
		isRunning = false;
		try {
			if (socket != null && !socket.isClosed())
				socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			instance = null;
		}
	}

	/**
	 * 消息分发处理
	 * <p>
	 * 【核心方法】根据消息类型分发给对应的监听器
	 * - 在线用户列表 -> OnlineUserListener
	 * - 聊天消息 -> 先缓存到ManageClientService，再通知PrivateMessageListener
	 * - 群聊创建 -> GroupCreateListener
	 * 
	 * @param msg 收到的消息
	 */
	private void handleMessage(Message msg) {
		String type = msg.getMesType();
		System.out.println(msg);
		switch (type) {
			case MessageType.MESSAGE_RET_ONLINE_FRIEND:
				// 在线用户列表更新
				for (OnlineUserListener l : onlineUserListeners) {
					l.onMessageReceived(msg);
				}
				break;
			case MessageType.MESSAGE_COMM_MES:
			case MessageType.MESSAGE_WORLD_CHAT:
			case MessageType.MESSAGE_GROUP_MES:
				// 将消息存入缓存区
				ManageClientService service = ManageClientService.getInstance();
				if (service != null) {
					String key;
					if (MessageType.MESSAGE_WORLD_CHAT.equals(type)) {
						key = "世界聊天";
					} else if (MessageType.MESSAGE_GROUP_MES.equals(type)) {
						// 群聊使用群成员列表生成唯一key
						List<String> members = msg.getUserlist();
						List<String> sortedMembers = new ArrayList<>(members);
						Collections.sort(sortedMembers);
						key = "群聊:" + String.join(",", sortedMembers);
					} else {
						key = msg.getSender();
					}
					service.addMessageToBuffer(key, msg);
				}
				// 通知监听器更新UI
				for (PrivateMessageListener l : privateMessageListeners) {
					l.onMessageReceived(msg);
				}
				break;
			case MessageType.MESSAGE_GROUP_CREATE:
				// 群聊创建通知
				for (GroupCreateListener l : groupCreateListeners) {
					l.onGroupCreated(msg);
				}
				break;
			case MessageType.MESSAGE_SHAKE:
				// 窗口抖动消息
				for (PrivateMessageListener l : privateMessageListeners) {
					l.onMessageReceived(msg);
				}
				break;
			case MessageType.MESSAGE_FILE:
				// 文件传输消息
				for (PrivateMessageListener l : privateMessageListeners) {
					l.onMessageReceived(msg);
				}
				break;
			case MessageType.MESSAGE_SYSTEM_BROADCAST:
				// 系统广播消息
				for (PrivateMessageListener l : privateMessageListeners) {
					l.onMessageReceived(msg);
				}
				break;
		}
	}
}
