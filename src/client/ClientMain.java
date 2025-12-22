package client;

import client.service.ClientConnectServer;
import client.service.ManageClientService;
import client.view.LoginView;
import client.view.MainChatView;
import client.view.RegisterDialog;
import common.Message;
import common.MessageType;
import common.User;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * 客户端主入口类 - 聊天客户端的启动类
 * <p>
 * 【核心作用】
 * 1. 作为程序入口，初始化登录界面
 * 2. 协调登录和注册的业务流程
 * 3. 建立与服务器的Socket连接
 * <p>
 * 【架构说明】采用MVC模式：
 * - View层：LoginView、MainChatView、RegisterDialog
 * - Service层：ClientConnectServer（网络通信）、ManageClientService（业务逻辑）
 * - Model层：User、Message（数据实体）
 * <p>
 * 【启动流程】
 * 1. 显示登录界面
 * 2. 用户输入账号密码后点击登录
 * 3. 创建Socket连接服务器
 * 4. 发送登录请求并验证
 * 5. 登录成功后打开主聊天界面
 * 
 * @author ChatRoom Team
 */
public class ClientMain {

	/**
	 * 构造函数 - 初始化客户端界面和事件绑定
	 * <p>
	 * 【核心逻辑】
	 * - 创建并显示登录界面
	 * - 绑定登录按钮点击事件
	 * - 绑定注册按钮点击事件
	 */
	public ClientMain() {
		// 1. 启动登录界面
		LoginView loginView = new LoginView();
		loginView.setVisible(true);

		// 2. 绑定登录按钮事件
		loginView.addLoginListener(new ActionListener() {
			/**
			 * 登录按钮点击事件处理
			 * 【关键流程】
			 * 1. 获取用户输入的ID和密码
			 * 2. 建立Socket连接
			 * 3. 发送登录请求
			 * 4. 根据响应结果跳转主界面或显示错误
			 */
			@Override
			public void actionPerformed(ActionEvent e) {
				String userId = loginView.getUserId();
				String pwd = loginView.getPassword();

				// 输入校验 - 防止空值提交
				if (userId.isEmpty() || pwd.isEmpty()) {
					loginView.showMessage("用户ID或密码不能为空");
					return;
				}

				try {
					// 3. 连接服务器
					int port = 8888;
					String ip = "127.0.0.1";
					Socket socket = new Socket(ip, port);

					// 创建User对象并获取单例连接实例
					User user = new User(userId, pwd);
					user.setUserID(userId);
					ClientConnectServer clientConnectServer = ClientConnectServer.getInstance(user, socket);

					if (clientConnectServer.sendLoginRequest()) {
						// 登录成功 - 关闭登录窗口，打开主聊天界面
						loginView.dispose();
						MainChatView mainChatView = new MainChatView(user);
						mainChatView.setVisible(true);
					} else {
						// 登录失败 - 显示错误信息并关闭连接
						loginView.showMessage("登录失败: 用户名或密码错误");
						socket.close();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					loginView.showMessage("无法连接服务器: " + ex.getMessage());
				}
			}
		});

		// 3. 绑定注册按钮事件
		loginView.addRegisterListener(new ActionListener() {
			/**
			 * 注册按钮点击事件处理
			 * 【关键流程】
			 * 1. 弹出注册对话框收集用户信息
			 * 2. 建立临时Socket连接
			 * 3. 发送注册请求
			 * 4. 接收服务器分配的UserID
			 */
			@Override
			public void actionPerformed(ActionEvent e) {
				RegisterDialog dialog = new RegisterDialog(loginView);
				dialog.setVisible(true);

				if (dialog.isConfirmed()) {
					String nickname = dialog.getNickname();
					String password = dialog.getPassword();
					String gender = dialog.getGender();

					try {
						// 建立临时连接发送注册请求
						int port = 8888;
						String ip = "127.0.0.1";
						Socket socket = new Socket(ip, port);

						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

						// 构建注册消息，content格式: "昵称|密码|性别"
						Message registerMsg = new Message.Builder()
								.mesType(MessageType.MESSAGE_REGISTER)
								.content(nickname + "|" + password + "|" + gender)
								.build();
						oos.writeObject(registerMsg);

						// 接收服务器响应
						Message response = (Message) ois.readObject();
						if (MessageType.MESSAGE_REGISTER_SUCCEED.equals(response.getMesType())) {
							// 注册成功 - 显示分配的UserID
							String userId = response.getContent();
							loginView.showMessage("注册成功！\n您的用户ID是: " + userId + "\n请牢记此ID用于登录");
						} else {
							loginView.showMessage("注册失败: " + response.getContent());
						}

						socket.close();
					} catch (Exception ex) {
						ex.printStackTrace();
						loginView.showMessage("无法连接服务器: " + ex.getMessage());
					}
				}
			}
		});
	}

	/**
	 * 程序入口方法
	 * 
	 * @param args 命令行参数（未使用）
	 */
	public static void main(String[] args) {
		new ClientMain();
	}
}
