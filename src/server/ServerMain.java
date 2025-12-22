package server;

import common.Message;
import common.MessageType;
import common.User;
import common.User;
import server.db.UserDao;
import server.service.ManageClientThread;
import server.service.ServerConnectClientThread;
import server.view.ServerGUI;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端主入口类 - 聊天服务器的核心启动类
 * <p>
 * 【核心作用】
 * 1. 启动服务端GUI监控界面
 * 2. 在8888端口监听客户端连接
 * 3. 处理用户登录和注册请求
 * 4. 为每个登录成功的用户创建独立的通信线程
 * <p>
 * 【启动流程】
 * 1. 创建并显示ServerGUI界面
 * 2. 创建ServerSocket监听8888端口
 * 3. 循环等待客户端连接（accept阻塞）
 * 4. 接收登录/注册请求并验证
 * 5. 登录成功后创建ServerConnectClientThread线程
 * 6. 广播在线用户列表给所有客户端
 * <p>
 * 【设计说明】
 * - 采用"每连接一线程"模型（one-thread-per-connection）
 * - 支持数据库认证（MySQL + c3p0连接池）
 * - 用户ID采用8位随机数生成，保证唯一性
 * 
 * @author ChatRoom Team
 */
public class ServerMain {
	/** 服务端GUI界面实例 */
	private static ServerGUI gui;

	public static void main(String[] args) {
		// 创建并显示GUI
		SwingUtilities.invokeLater(() -> {
			gui = new ServerGUI();
			gui.setVisible(true);
		});

		// 等待GUI初始化
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		new ServerMain();
	}

	public ServerMain() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(8888);
			gui.appendLog("服务器在8888端口监听...");

			while (true) {
				Socket socket = serverSocket.accept();
				gui.appendLog("客户端连接: " + socket.getInetAddress());
				ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

				try {
					// A. 接收客户端发来的登录/注册请求
					Message msg = (Message) ois.readObject();
					if (MessageType.MESSAGE_LOGIN.equals(msg.getMesType())) {
						// B. 登录请求
						String userId = msg.getSender();
						String pwd = msg.getContent();
						gui.appendLog("用户 " + userId + " 请求登录");

						if (checkUser(userId, pwd)) {
							gui.appendLog("用户 " + userId + " 登录成功");

							// 获取用户信息以获取昵称
							UserDao userDao = new UserDao();
							User dbUser = userDao.getUserById(userId);
							userDao.closeConnection();
							String nickname = (dbUser != null) ? dbUser.getNicname() : userId;

							// A. 回复登录成功消息，并在content中携带昵称
							Message replyMsg = new Message.Builder()
									.mesType(MessageType.MESSAGE_LOGIN_SUCCEED)
									.content(nickname)
									.build();
							oos.writeObject(replyMsg);

							// C. 创建专门为该用户服务的线程
							ServerConnectClientThread thread = new ServerConnectClientThread(socket, userId, oos, ois,
									gui);
							thread.start();
							ManageClientThread.addClientThread(userId, thread);

							// D. 主动给所有客户端发送在线用户列表
							// 1. 获取最新的用户列表（昵称+ID格式）
							List<String> onlineUsersList = thread.getDetailedOnlineUsers();

							// 2. 创建更新消息
							Message updateListMsg = new Message.Builder()
									.mesType(MessageType.MESSAGE_RET_ONLINE_FRIEND)
									.setUserlist(onlineUsersList)
									.build();

							// 3. 遍历所有线程并发送
							ConcurrentHashMap<String, ServerConnectClientThread> allThreads = ManageClientThread
									.getAll();

							for (ServerConnectClientThread clientThread : allThreads.values()) {
								try {
									// 每个客户端都收到完整的最新列表
									clientThread.getOOS().writeObject(updateListMsg);
								} catch (IOException e) {
									// 如果发送失败，说明该客户端可能已掉线，可以记录日志或处理
									gui.appendLog("向用户 " + clientThread.getName() + " 发送在线列表失败: " + e.getMessage());
								}
							}
							// 更新GUI用户列表
							gui.updateUserList(onlineUsersList);

						} else {
							gui.appendLog("用户 " + userId + " 登录失败");
							Message replyMsg = new Message.Builder().mesType(MessageType.MESSAGE_LOGIN_FAIL).build();
							oos.writeObject(replyMsg);
							socket.close();
						}
					} else if (MessageType.MESSAGE_REGISTER.equals(msg.getMesType())) {
						// 处理注册请求
						String content = msg.getContent(); // 格式: "昵称|密码|性别"
						String[] parts = content.split("\\|");
						if (parts.length == 3) {
							String nickname = parts[0];
							String password = parts[1];
							String gender = parts[2];

							// 调用注册服务
							String newUserId = registerUser(nickname, password, gender);
							if (newUserId != null) {
								// 返回成功消息
								Message regSucceedMsg = new Message.Builder()
										.mesType(MessageType.MESSAGE_REGISTER_SUCCEED)
										.content(newUserId)
										.build();
								oos.writeObject(regSucceedMsg);
								gui.appendLog("用户注册成功: " + nickname + " -> ID: " + newUserId);
							} else {
								Message regFailMsg = new Message.Builder()
										.mesType(MessageType.MESSAGE_REGISTER_FAIL)
										.content("注册失败，请稍后再试或用户ID已存在")
										.build();
								oos.writeObject(regFailMsg);
								gui.appendLog("注册失败: " + nickname);
							}
						} else {
							Message replyMsg = new Message.Builder()
									.mesType(MessageType.MESSAGE_REGISTER_FAIL)
									.content("注册信息格式错误")
									.build();
							oos.writeObject(replyMsg);
							gui.appendLog("注册信息格式错误");
						}
						socket.close();
					} else {
						gui.appendLog("收到非法的登录请求");
						socket.close();
					}
				} catch (Exception e) {
					gui.appendLog("处理客户端请求异常: " + e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			gui.appendLog("服务器异常: " + e.getMessage());
			e.printStackTrace();
		} finally {
			try {
				if (serverSocket != null)
					serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private boolean checkUser(String userId, String pwd) throws SQLException {
		UserDao userDao = new UserDao();
		try {
			return userDao.checkUserById(userId, pwd);
		} finally {
			userDao.closeConnection();
		}
	}

	/**
	 * 注册用户
	 * 
	 * @param nickname 昵称
	 * @param password 密码
	 * @param gender   性别
	 * @return 生成的8位用户ID，失败返回null
	 * @throws SQLException
	 */
	private String registerUser(String nickname, String password, String gender) throws SQLException {
		int randomId = 10000000 + (int) (Math.random() * 90000000);
		String userId = String.valueOf(randomId);
		UserDao userDao = new UserDao();
		try {
			if (userDao.checkIDUnique(userId)) {
				userDao.RegisterUser(userId, nickname, password, gender);
				return userId;
			} else {
				return registerUser(nickname, password, gender);
			}
		} finally {
			userDao.closeConnection();
		}
	}
}
