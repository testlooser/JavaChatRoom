package client.view;

import client.service.ManageClientService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 登录界面 - 用户登录和注册入口
 * <p>
 * 【核心作用】
 * 1. 提供用户ID和密码输入界面
 * 2. 登录按钮触发服务器连接和认证
 * 3. 注册按钮弹出注册对话框
 * <p>
 * 【界面组成】
 * - 用户ID输入框
 * - 密码输入框
 * - 登录按钮
 * - 注册按钮
 * <p>
 * 【设计模式】
 * - MVC模式：View层只负责界面展示
 * - 业务逻辑由ClientMain通过监听器处理
 * 
 * @author ChatRoom Team
 */
public class LoginView extends JFrame {

	private final JTextField usernameField;
	private final JPasswordField passwordField;
	private final JButton loginButton;
	private final JButton registerButton;

	public LoginView() {
		setTitle("客户端登录");
		setSize(300, 180);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.fill = GridBagConstraints.HORIZONTAL;

		gbc.gridx = 0;
		gbc.gridy = 0;
		panel.add(new JLabel("用户ID:"), gbc);
		gbc.gridx = 1;
		usernameField = new JTextField(15);
		panel.add(usernameField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		panel.add(new JLabel("密码:"), gbc);
		gbc.gridx = 1;
		passwordField = new JPasswordField(15);
		panel.add(passwordField, gbc);

		gbc.gridx = 0;
		gbc.gridy = 2;
		loginButton = new JButton("登录");
		panel.add(loginButton, gbc);

		gbc.gridx = 1;
		registerButton = new JButton("注册");
		panel.add(registerButton, gbc);

		add(panel);
	}

	public String getUserId() {
		return usernameField.getText().trim();
	}

	public String getPassword() {
		return new String(passwordField.getPassword());
	}

	public void addLoginListener(ActionListener listener) {
		loginButton.addActionListener(listener);
	}

	public void addRegisterListener(ActionListener listener) {
		registerButton.addActionListener(listener);
	}

	public void showMessage(String message) {
		JOptionPane.showMessageDialog(this, message);
	}
}
