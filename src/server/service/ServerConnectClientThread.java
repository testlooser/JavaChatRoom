package server.service;

import common.Message;
import common.MessageType;
import common.User;
import server.db.UserDao;
import server.view.ServerGUI;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 服务端客户端通信线程 - 为每个在线用户提供独立的消息处理线程
 * <p>
 * 【核心作用】
 * 1. 持续监听该用户发送的消息
 * 2. 根据消息类型分发处理（私聊、群聊、世界聊天等）
 * 3. 将消息转发给目标用户或广播给所有用户
 * <p>
 * 【消息处理流程】
 * 1. run()方法循环读取ObjectInputStream
 * 2. 根据MessageType分发到对应的handle方法
 * 3. 查询目标用户的线程，通过其ObjectOutputStream转发消息
 * <p>
 * 【消息转发逻辑】
 * - 私聊消息：直接转发给接收者线程
 * - 世界聊天：遍历所有线程广播（除发送者外）
 * - 群聊消息：遍历群成员列表定向转发
 * - 用户下线：从管理器移除线程，广播更新在线列表
 * <p>
 * 【线程生命周期】
 * - 创建时机：用户登录成功后由ServerMain创建
 * - 终止时机：收到MESSAGE_CLIENT_EXIT或连接异常
 * 
 * @author ChatRoom Team
 */
public class ServerConnectClientThread extends Thread {
	private final Socket socket;
	private final String userId;
	private final ObjectOutputStream oos;
	private final ObjectInputStream ois;
	private final ServerGUI gui;

	public ServerConnectClientThread(Socket socket, String userId, ObjectOutputStream oos, ObjectInputStream ois,
			ServerGUI gui) {
		this.socket = socket;
		this.userId = userId;
		this.oos = oos;
		this.ois = ois;
		this.gui = gui;
		try {
			postPendingMessages();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		gui.appendLog("用户 " + userId + " 上线");
	}

	public ObjectOutputStream getOOS() {
		return oos;
	}

	public void postPendingMessages() throws IOException {
		List<Message> pending = ManageClientThread.offlineMessages.get(userId);
		if (pending != null) {
			for (Message m : pending) {
				oos.writeObject(m);
			}
			ManageClientThread.offlineMessages.remove(userId);
		}
	}

	@Override
	public void run() {
		System.out.println("服务端线程启动，等待用户 " + userId + " 消息...");
		while (true) {
			try {
				Message msg = (Message) ois.readObject();
				String type = msg.getMesType();
				System.out.println("用户 " + userId + " 消息类型: " + type);

				switch (type) {
					case MessageType.MESSAGE_COMM_MES:
						handlePrivateMessage(msg);
						break;
					case MessageType.MESSAGE_WORLD_CHAT:
						handleWorldMessage(msg);
						break;
					case MessageType.MESSAGE_GROUP_MES:
						handleGroupMessage(msg);
						break;
					case MessageType.MESSAGE_GROUP_CREATE:
						handleGroupCreate(msg);
						break;
					case MessageType.MESSAGE_SHAKE:
						handlePrivateMessage(msg);
						break;
					case MessageType.MESSAGE_FILE:
						handlePrivateMessage(msg);
						break;
					case MessageType.MESSAGE_GET_ONLINE_FRIEND:
						handleOnlineUserRequest(msg);
						break;
					case MessageType.MESSAGE_CLIENT_EXIT:
						handleClientExit();
						return;
					default:
						System.out.println("无法处理的消息类型: " + type);
				}
			} catch (Exception e) {
				gui.appendLog("用户 " + userId + " 连接异常或意外退出");
				ManageClientThread.removeClientThread(userId);
				broadcastOnlineUsers();
				gui.updateUserList(getDetailedOnlineUsers());
				try {
					socket.close();
				} catch (Exception ignored) {
				}
				break;
			}
		}
	}

	private void handlePrivateMessage(Message msg) throws IOException {
		String receiverId = msg.getReceiver();
		ServerConnectClientThread receiverThread = ManageClientThread.getClientThread(receiverId);

		if (receiverId.equals(userId)) {
			gui.appendLog(userId + " 给自己发送消息: " + msg.getContent());
		} else if (receiverThread != null) {
			receiverThread.getOOS().writeObject(msg);
			// 记录日志 - 根据消息类型显示不同内容
			String logMsg = formatMessageLog(msg);
			gui.appendLog(logMsg);
		} else {
			ManageClientThread.offlineMessages
					.computeIfAbsent(receiverId, k -> new ArrayList<>())
					.add(msg);
			gui.appendLog(userId + " -> " + receiverId + ": (用户离线，消息已缓存)");
		}
	}

	private void handleWorldMessage(Message msg) throws IOException {
		gui.appendLog(userId + ": [世界聊天] \"" + msg.getContent() + "\"");
		for (var entry : ManageClientThread.getAll().entrySet()) {
			String targetUser = entry.getKey();
			ServerConnectClientThread thread = entry.getValue();
			if (!targetUser.equals(userId)) {
				thread.getOOS().writeObject(msg);
			}
		}
	}

	private void handleGroupMessage(Message msg) throws IOException {
		List<String> groupMembers = msg.getUserlist();
		gui.appendLog(userId + " -> 群组: [群聊消息] \"" + msg.getContent() + "\"");
		for (String member : groupMembers) {
			if (!member.equals(userId)) {
				ServerConnectClientThread thread = ManageClientThread.getClientThread(member);
				if (thread != null) {
					thread.getOOS().writeObject(msg);
				}
			}
		}
	}

	private void handleGroupCreate(Message msg) throws IOException {
		List<String> groupMembers = msg.getUserlist();
		System.out.println("【创建群聊】" + msg.getSender() + " 创建群聊: " + groupMembers);
		// 广播给所有群成员（包括创建者，以便同步）
		for (String member : groupMembers) {
			ServerConnectClientThread thread = ManageClientThread.getClientThread(member);
			if (thread != null) {
				thread.getOOS().writeObject(msg);
			}
		}
	}

	private void handleOnlineUserRequest(Message msg) throws IOException {
		List<String> onlineUserList = getDetailedOnlineUsers();
		Message resMsg = new Message.Builder()
				.mesType(MessageType.MESSAGE_RET_ONLINE_FRIEND)
				.setUserlist(onlineUserList)
				.receiver(msg.getSender())
				.build();
		oos.writeObject(resMsg);
	}

	private void handleClientExit() throws IOException {
		gui.appendLog("用户 " + userId + " 下线");
		ManageClientThread.removeClientThread(userId);
		socket.close();
		broadcastOnlineUsers();
		gui.updateUserList(getDetailedOnlineUsers());
	}

	private void broadcastOnlineUsers() {
		try {
			List<String> onlineUserList = getDetailedOnlineUsers();
			Message onlineMsg = new Message.Builder()
					.mesType(MessageType.MESSAGE_RET_ONLINE_FRIEND)
					.setUserlist(onlineUserList)
					.build();
			for (var entry : ManageClientThread.getAll().entrySet()) {
				entry.getValue().getOOS().writeObject(onlineMsg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获取详细的在线用户列表（格式："昵称(UserID)"）
	 */
	public List<String> getDetailedOnlineUsers() {
		List<String> onlineUserIds = ManageClientThread.getOnlineUsers();
		List<String> detailedList = new ArrayList<>();

		for (String userId : onlineUserIds) {
			try {
				UserDao userDao = new UserDao();
				User user = userDao.getUserById(userId);
				userDao.closeConnection();

				if (user != null && user.getNicname() != null) {
					// 格式："昵称(UserID)"
					detailedList.add(user.getNicname() + "(" + userId + ")");
				} else {
					// 如果没有昵称，只显示ID
					detailedList.add(userId);
				}
			} catch (SQLException e) {
				// 数据库查询失败，只显示ID
				detailedList.add(userId);
				e.printStackTrace();
			}
		}
		return detailedList;
	}

	/**
	 * 格式化消息日志
	 */
	private String formatMessageLog(Message msg) {
		String sender = msg.getSender();
		String receiver = msg.getReceiver();
		String type = msg.getMesType();

		switch (type) {
			case MessageType.MESSAGE_COMM_MES:
				return sender + " -> " + receiver + ": [普通文字] \"" + msg.getContent() + "\"";
			case MessageType.MESSAGE_FILE:
				long fileSize = msg.getFileData() != null ? msg.getFileData().length / 1024 : 0;
				return sender + " -> " + receiver + ": [文件传输] " + msg.getFileName() + " (" + fileSize + "KB)";
			case MessageType.MESSAGE_SHAKE:
				return sender + " -> " + receiver + ": [窗口抖动]";
			default:
				return sender + " -> " + receiver + ": [" + type + "]";
		}
	}
}
