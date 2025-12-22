package client.view;

import client.model.ChatHistoryManager;
import client.service.ClientConnectServer;
import client.service.ManageClientService;
import client.utils.TrayManager;
import client.utils.SoundManager;
import client.utils.ScreenshotUtil;
import common.Message;
import common.MessageType;
import common.User;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主聊天界面 - 客户端的核心聊天窗口
 * <p>
 * 【核心作用】
 * 1. 显示聊天消息（私聊、群聊、世界聊天）
 * 2. 发送文字消息（支持富文本样式）
 * 3. 发送文件和截图
 * 4. 管理在线用户列表和群聊列表
 * <p>
 * 【界面布局】
 * - 左侧面板：个人信息、在线用户列表、群聊列表
 * - 右侧面板：聊天标题、消息显示区、输入区
 * <p>
 * 【功能特性】
 * - 私聊：双击在线用户发起私聊
 * - 群聊：创建群聊并发送群消息
 * - 世界聊天：广播给所有在线用户
 * - 文件传输：选择文件发送给私聊对象
 * - 截图发送：框选屏幕区域发送
 * - 窗口抖动：发送抖动效果给私聊对象
 * - 未读消息计数：显示各聊天的未读消息数
 * - 消息缓存：聊天记录本地持久化
 * <p>
 * 【消息监听】
 * 通过ClientConnectServer的监听器接收服务器消息，
 * 根据消息类型更新UI或缓存消息
 * 
 * @author ChatRoom Team
 */
public class MainChatView extends JFrame {

	private final DefaultListModel<String> listModel = new DefaultListModel<>();
	private final JList<String> userList = new JList<>(listModel);
	private final JButton worldChatButton = new JButton("世界聊天");
	private final JButton createGroupButton = new JButton("新建群聊");
	private final ManageClientService manageClientService;
	private final TrayManager trayManager;
	private final JPanel groupListPanel = new JPanel();
	private final List<List<String>> groupChats = new ArrayList<>();

	private final JTextPane chatArea = new JTextPane();
	private final JTextField inputField = new JTextField();
	private final JButton sendTextBtn = new JButton("发送文字");
	private final JButton shakeBtn = new JButton("抖一抖");
	private final JButton sendFileBtn = new JButton("发送文件");
	private final JButton screenshotBtn = new JButton("截图");

	private final JToolBar styleToolbar = new JToolBar();
	private JComboBox<Integer> fontSizeCombo;
	private final JButton boldBtn = new JButton("B");
	private final JButton colorBtn = new JButton("颜色");

	private Color currentColor = Color.BLACK;
	private boolean currentBold = false;
	private int currentFontSize = 14;

	private final User user;
	private String chatWith = "世界聊天"; // 默认世界聊天
	private boolean isWorldChat = true;
	private boolean isGroupChat = false;
	private List<String> currentGroupMembers = new ArrayList<>();

	// 未读消息计数
	private final Map<String, Integer> unreadCounts = new HashMap<>();
	private final Map<String, JButton> groupButtonMap = new HashMap<>();

	// ID → 昵称映射（用于显示）
	private final Map<String, String> userIdToNickname = new HashMap<>();
	private final Map<String, String> nicknameToUserId = new HashMap<>();

	public MainChatView(User user) throws IOException {
		this.user = user;
		this.manageClientService = ManageClientService.getInstance(user);
		this.trayManager = new TrayManager(this);
		setTitle("聊天系统 - " + user.getName());
		setSize(900, 600);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setLayout(new BorderLayout());

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				ManageClientService.getInstance().sendExitRequest();
				System.exit(0);
			}

			@Override
			public void windowIconified(WindowEvent e) {
				// 最小化时，可选：最小化到托盘
				trayManager.minimizeToTray();
			}
		});

		initLeftPanel();
		initRightPanel();

		// ===== 注册消息监听器 =====
		ClientConnectServer.getInstance().addPrivateMessageListener(msg -> SwingUtilities.invokeLater(() -> {
			String sender = msg.getSender();
			boolean display = false;

			// 处理抖动消息
			if (MessageType.MESSAGE_SHAKE.equals(msg.getMesType())) {
				shakeWindow();
				return;
			}

			// 处理文件消息
			if (MessageType.MESSAGE_FILE.equals(msg.getMesType())) {
				receiveFile(msg);
				return;
			}

			// 处理系统广播消息
			if (MessageType.MESSAGE_SYSTEM_BROADCAST.equals(msg.getMesType())) {
				Date sendTime = msg.getSendTime();
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
				String timeStr = sdf.format(sendTime);
				appendChat("【系统消息】", msg.getContent(), Color.RED, true, 16, timeStr);
				SoundManager.beep(); // 系统消息提示音
				return;
			}

			// 处理群组创建消息
			if (MessageType.MESSAGE_GROUP_CREATE.equals(msg.getMesType())) {
				List<String> groupMembers = msg.getUserlist();
				// 检查是否包含自己
				boolean containsSelf = false;
				for (String member : groupMembers) {
					String memberId = extractUserId(member);
					if (memberId.equals(user.getUserID())) {
						containsSelf = true;
						break;
					}
				}
				if (containsSelf) {
					// 添加到群聊列表
					groupChats.add(new ArrayList<>(groupMembers));
					updateGroupList();
				}
				return;
			}

			if (MessageType.MESSAGE_WORLD_CHAT.equals(msg.getMesType()) && isWorldChat) {
				display = true;
			} else if (MessageType.MESSAGE_COMM_MES.equals(msg.getMesType())) {
				// 修复：比较UserID而不是带昵称的字符串
				String currentChatUserId = extractUserId(chatWith);
				if (sender.equals(currentChatUserId)) {
					display = true;
				}
			} else if (MessageType.MESSAGE_GROUP_MES.equals(msg.getMesType()) && isGroupChat) {
				// 检查是否是当前群聊的消息
				List<String> msgMembers = msg.getUserlist();
				List<String> sortedMsg = new ArrayList<>(msgMembers);
				Collections.sort(sortedMsg);
				List<String> sortedCurrent = new ArrayList<>(currentGroupMembers);
				Collections.sort(sortedCurrent);
				if (sortedMsg.equals(sortedCurrent)) {
					display = true;
				}
			}

			if (display) {
				Color c = Color.BLACK;
				boolean b = false;
				int size = 14;
				String sendTimeStr = "";
				try {
					Date sendTime = msg.getSendTime();
					SimpleDateFormat sendTimeFormat = new SimpleDateFormat("HH:mm");
					sendTimeStr = sendTimeFormat.format(sendTime);
					c = Color.decode(msg.getFontColor());
					b = msg.isBold();
					size = msg.getFontSize();
				} catch (Exception ignored) {
				}
				appendChat(sender, msg.getContent(), c, b, size, sendTimeStr);
				// 播放消息提示音
				SoundManager.playMessageSound();
			} else {
				// 未显示的消息，增加未读计数
				String unreadKey = getUnreadKey(msg);
				if (unreadKey != null) {
					unreadCounts.merge(unreadKey, 1, Integer::sum);
					updateUnreadIndicators();
				}
			}
		}));

		// ===== 注册在线用户监听器 =====
		ClientConnectServer.getInstance().addOnlineUserListener(msg -> {
			if (MessageType.MESSAGE_RET_ONLINE_FRIEND.equals(msg.getMesType())) {
				List<String> users = msg.getUserlist();
				SwingUtilities.invokeLater(() -> updateOnlineUsers(users));
			}
		});

		// ===== 注册群聊创建监听器 =====
		ClientConnectServer.getInstance().addGroupCreateListener(msg -> {
			List<String> members = msg.getUserlist();
			SwingUtilities.invokeLater(() -> addGroupToList(members));
		});

		manageClientService.requestOnlineUsers();
		setVisible(true);
	}

	private void initLeftPanel() {
		JPanel leftPanel = new JPanel(new BorderLayout());

		// ========== 顶部：个人信息区域 ==========
		JPanel personalInfoPanel = new JPanel();
		personalInfoPanel.setLayout(new BoxLayout(personalInfoPanel, BoxLayout.Y_AXIS));
		personalInfoPanel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createTitledBorder("个人信息"),
				BorderFactory.createEmptyBorder(5, 10, 5, 10)));
		personalInfoPanel.setBackground(new Color(240, 248, 255)); // 浅蓝色背景

		JLabel nicknameLabel = new JLabel("昵称: " + user.getName());
		nicknameLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
		nicknameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel userIdLabel = new JLabel("ID: " + user.getUserID());
		userIdLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
		userIdLabel.setForeground(Color.GRAY);
		userIdLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

		personalInfoPanel.add(nicknameLabel);
		personalInfoPanel.add(Box.createVerticalStrut(5)); // 间距
		personalInfoPanel.add(userIdLabel);
		personalInfoPanel.setPreferredSize(new Dimension(200, 70));
		personalInfoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

		// ========== 中间：在线用户区域 ==========
		JPanel userSection = new JPanel(new BorderLayout());
		JLabel userTitle = new JLabel("在线用户", SwingConstants.CENTER);
		userTitle.setFont(new Font("微软雅黑", Font.BOLD, 14));
		userTitle.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		userSection.add(userTitle, BorderLayout.NORTH);

		userList.setModel(listModel);
		userList.setFont(new Font("微软雅黑", Font.PLAIN, 13));
		userList.setFixedCellHeight(30);
		userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		userList.setCellRenderer(new UnreadCellRenderer());
		JScrollPane userScroll = new JScrollPane(userList);
		userSection.add(userScroll, BorderLayout.CENTER);

		// ========== 底部：群聊列表区域 ==========
		JPanel groupSection = new JPanel(new BorderLayout());
		JLabel groupTitle = new JLabel("群聊列表", SwingConstants.CENTER);
		groupTitle.setFont(new Font("微软雅黑", Font.BOLD, 14));
		groupTitle.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
		groupSection.add(groupTitle, BorderLayout.NORTH);

		groupListPanel.setLayout(new BoxLayout(groupListPanel, BoxLayout.Y_AXIS));
		JScrollPane groupScroll = new JScrollPane(groupListPanel);
		groupScroll.setPreferredSize(new Dimension(200, 150));
		groupSection.add(groupScroll, BorderLayout.CENTER);

		// ========== 组合中间内容：在线用户 + 群聊列表 ==========
		JPanel centerContent = new JPanel(new BorderLayout());
		centerContent.add(userSection, BorderLayout.CENTER);
		centerContent.add(groupSection, BorderLayout.SOUTH);

		// ========== 最底部：按钮区域 ==========
		JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 5, 5));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		bottomPanel.add(worldChatButton);
		bottomPanel.add(createGroupButton);

		// ========== 组合左侧整体面板 ==========
		leftPanel.add(personalInfoPanel, BorderLayout.NORTH);
		leftPanel.add(centerContent, BorderLayout.CENTER);
		leftPanel.add(bottomPanel, BorderLayout.SOUTH);

		add(leftPanel, BorderLayout.WEST);
		leftPanel.setPreferredSize(new Dimension(200, 0));

		// ========== 事件监听器 ==========
		userList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					String selectedUser = userList.getSelectedValue();
					if (selectedUser != null) {
						// 提取UserID用于发送消息
						String targetUserId = extractUserId(selectedUser);
						if (!targetUserId.equals(user.getUserID())) {
							chatWith = targetUserId;
							isWorldChat = false;
							isGroupChat = false;
							((JLabel) ((JPanel) getContentPane().getComponent(1)).getComponent(0))
									.setText("私聊: " + selectedUser);
							clearUnreadAndRefresh(targetUserId);
							loadChatHistory(targetUserId);
						}
					}
				}
			}
		});

		worldChatButton.addActionListener(e -> {
			chatWith = "世界聊天";
			isWorldChat = true;
			isGroupChat = false;
			currentGroupMembers.clear();
			((JLabel) ((JPanel) getContentPane().getComponent(1)).getComponent(0)).setText(chatWith);
			clearUnreadAndRefresh("世界聊天");
			loadChatHistory("世界聊天");
		});

		createGroupButton.addActionListener(e -> showCreateGroupDialog());
	}

	/**
	 * 更新群聊列表
	 */
	private void updateGroupList() {
		SwingUtilities.invokeLater(() -> {
			groupListPanel.removeAll();
			groupButtonMap.clear();
			// 重新添加所有群
			for (List<String> group : groupChats) {
				addGroupToList(group);
			}
			groupListPanel.revalidate();
			groupListPanel.repaint();
		});
	}

	private void addGroupToList(List<String> members) {
		// 检查是否已存在
		List<String> sorted = new ArrayList<>(members);
		Collections.sort(sorted);
		for (List<String> existing : groupChats) {
			List<String> existingSorted = new ArrayList<>(existing);
			Collections.sort(existingSorted);
			if (sorted.equals(existingSorted)) {
				return; // 已存在
			}
		}
		groupChats.add(members);

		// 生成群名（排除自己）
		List<String> othersInGroup = new ArrayList<>();
		for (String memberId : members) {
			if (!memberId.equals(user.getUserID())) {
				String nickname = userIdToNickname.getOrDefault(memberId, memberId);
				othersInGroup.add(nickname);
			}
		}
		String groupName = String.join(",", othersInGroup);
		if (groupName.length() > 15) {
			groupName = groupName.substring(0, 12) + "...";
		}

		JButton groupBtn = new JButton("群: " + groupName);
		groupBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
		groupBtn.setMaximumSize(new Dimension(180, 30));
		groupBtn.setName("群: " + groupName); // 保存原始名称
		final List<String> groupMembers = members;
		final String groupKey = "群聊:" + String.join(",", sorted);
		groupButtonMap.put(groupKey, groupBtn);

		groupBtn.addActionListener(e -> {
			currentGroupMembers = new ArrayList<>(groupMembers);
			isGroupChat = true;
			isWorldChat = false;

			List<String> sortedKey = new ArrayList<>(groupMembers);
			Collections.sort(sortedKey);
			chatWith = "群聊:" + String.join(",", sortedKey);

			((JLabel) ((JPanel) getContentPane().getComponent(1)).getComponent(0))
					.setText("群聊 (" + (groupMembers.size() - 1) + "人)");
			clearUnreadAndRefresh(chatWith);
			loadChatHistory(chatWith);
		});

		groupListPanel.add(groupBtn);
		groupListPanel.revalidate();
		groupListPanel.repaint();
	}

	private void initRightPanel() {
		JPanel rightPanel = new JPanel(new BorderLayout());

		JLabel chatTitle = new JLabel(chatWith, SwingConstants.CENTER);
		chatTitle.setFont(new Font("微软雅黑", Font.BOLD, 16));
		chatTitle.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		rightPanel.add(chatTitle, BorderLayout.NORTH);

		chatArea.setEditable(false);
		rightPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

		styleToolbar.setFloatable(false);
		fontSizeCombo = new JComboBox<>(new Integer[] { 12, 14, 16, 18, 20, 24, 28 });
		fontSizeCombo.setSelectedItem(currentFontSize);
		styleToolbar.add(new JLabel("字号:"));
		styleToolbar.add(fontSizeCombo);
		styleToolbar.add(boldBtn);
		styleToolbar.add(colorBtn);

		fontSizeCombo.addActionListener(e -> currentFontSize = (Integer) fontSizeCombo.getSelectedItem());
		boldBtn.addActionListener(e -> currentBold = !currentBold);
		colorBtn.addActionListener(e -> {
			Color selected = JColorChooser.showDialog(this, "选择文字颜色", currentColor);
			if (selected != null)
				currentColor = selected;
		});

		JPanel inputPanel = new JPanel(new BorderLayout());
		inputPanel.add(styleToolbar, BorderLayout.NORTH);

		JPanel bottomInputPanel = new JPanel(new BorderLayout());
		bottomInputPanel.add(inputField, BorderLayout.CENTER);

		JPanel btnPanel = new JPanel(new GridLayout(1, 4, 5, 5));
		btnPanel.add(sendTextBtn);
		btnPanel.add(shakeBtn);
		btnPanel.add(sendFileBtn);
		btnPanel.add(screenshotBtn);
		bottomInputPanel.add(btnPanel, BorderLayout.EAST);
		inputPanel.add(bottomInputPanel, BorderLayout.SOUTH);

		rightPanel.add(inputPanel, BorderLayout.SOUTH);
		add(rightPanel, BorderLayout.CENTER);

		sendTextBtn.addActionListener(e -> sendMessage());
		shakeBtn.addActionListener(e -> sendShake());
		sendFileBtn.addActionListener(e -> sendFile());
		screenshotBtn.addActionListener(e -> sendScreenshot());
	}

	/**
	 * 发送文件（使用独立线程）
	 */
	private void sendFile() {
		if (isWorldChat) {
			JOptionPane.showMessageDialog(this, "世界聊天不支持文件发送", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (isGroupChat) {
			JOptionPane.showMessageDialog(this, "群聊不支持文件发送", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// 选择文件
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("选择要发送的文件");
		int result = fileChooser.showOpenDialog(this);

		if (result != JFileChooser.APPROVE_OPTION) {
			return;
		}

		java.io.File file = fileChooser.getSelectedFile();

		// 检查文件大小（限制5MB）
		final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
		if (file.length() > MAX_FILE_SIZE) {
			JOptionPane.showMessageDialog(this,
					"文件大小超过5MB限制\n当前大小: " + (file.length() / 1024 / 1024) + "MB",
					"文件太大", JOptionPane.ERROR_MESSAGE);
			return;
		}

		// 在聊天面板显示"正在发送..."
		String statusMsg = "[正在发送文件: " + file.getName() + "]";
		appendChat("系统", statusMsg, Color.GRAY, false, 12, new java.text.SimpleDateFormat("HH:mm").format(new Date()));

		// 使用独立线程发送文件
		final java.io.File selectedFile = file;
		new Thread(() -> {
			try {
				// 读取文件
				byte[] fileData = java.nio.file.Files.readAllBytes(selectedFile.toPath());

				// 构建文件消息
				Message fileMsg = new Message.Builder()
						.mesType(MessageType.MESSAGE_FILE)
						.sender(user.getUserID())
						.receiver(chatWith)
						.fileName(selectedFile.getName())
						.fileData(fileData)
						.build();

				// 发送
				manageClientService.sendPrivateMessage(fileMsg);

				// 在GUI线程更新状态
				SwingUtilities.invokeLater(() -> {
					String successMsg = "[文件发送成功: " + selectedFile.getName() + " (" + (fileData.length / 1024) + "KB)]";
					appendChat("系统", successMsg, new Color(0, 128, 0), false, 12,
							new java.text.SimpleDateFormat("HH:mm").format(new Date()));
				});

			} catch (Exception ex) {
				ex.printStackTrace();
				// 在GUI线程显示错误
				SwingUtilities.invokeLater(() -> {
					String errorMsg = "[文件发送失败: " + ex.getMessage() + "]";
					appendChat("系统", errorMsg, Color.RED, false, 12,
							new java.text.SimpleDateFormat("HH:mm").format(new Date()));
				});
			}
		}, "FileTransferThread").start();
	}

	/**
	 * 截图并发送
	 */
	private void sendScreenshot() {
		if (isWorldChat) {
			JOptionPane.showMessageDialog(this, "世界聊天不支持截图发送", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (isGroupChat) {
			JOptionPane.showMessageDialog(this, "群聊不支持截图发送", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// 隐藏当前窗口
		setVisible(false);

		new Thread(() -> {
			try {
				Thread.sleep(300); // 等待窗口隐藏

				// 截图
				java.awt.image.BufferedImage screenshot = ScreenshotUtil.captureArea();

				// 恢复窗口
				SwingUtilities.invokeLater(() -> setVisible(true));

				if (screenshot == null) {
					return; // 用户取消
				}

				// 转为字节数组
				byte[] fileData = ScreenshotUtil.imageToBytes(screenshot);
				String fileName = "screenshot_" + System.currentTimeMillis() + ".png";

				// 构建文件消息
				Message fileMsg = new Message.Builder()
						.mesType(MessageType.MESSAGE_FILE)
						.sender(user.getUserID())
						.receiver(chatWith)
						.fileName(fileName)
						.fileData(fileData)
						.build();

				// 发送
				manageClientService.sendPrivateMessage(fileMsg);

				// 显示成功
				SwingUtilities.invokeLater(() -> {
					String successMsg = "[截图发送成功: " + fileName + " (" + (fileData.length / 1024) + "KB)]";
					appendChat("系统", successMsg, new Color(0, 128, 0), false, 12,
							new java.text.SimpleDateFormat("HH:mm").format(new Date()));
				});

			} catch (Exception ex) {
				ex.printStackTrace();
				SwingUtilities.invokeLater(() -> {
					setVisible(true);
					String errorMsg = "[截图发送失败: " + ex.getMessage() + "]";
					appendChat("系统", errorMsg, Color.RED, false, 12,
							new java.text.SimpleDateFormat("HH:mm").format(new Date()));
				});
			}
		}, "ScreenshotThread").start();
	}

	private void sendMessage() {
		String text = inputField.getText();
		if (text == null || text.trim().isEmpty())
			return;

		String timeStr = new SimpleDateFormat("HH:mm").format(new Date());
		appendChat(user.getName(), text, currentColor, currentBold, currentFontSize, timeStr);

		Message message;
		String colorHex = String.format("#%02x%02x%02x",
				currentColor.getRed(),
				currentColor.getGreen(),
				currentColor.getBlue());

		if (isWorldChat) {
			message = new Message.Builder()
					.mesType(MessageType.MESSAGE_WORLD_CHAT)
					.content(text)
					.sender(user.getUserID()) // 发送时使用UserID，接收端通过UserID显示昵称
					.fontColor(colorHex)
					.bold(currentBold)
					.fontSize(currentFontSize)
					.build();
		} else if (isGroupChat) {
			message = new Message.Builder()
					.mesType(MessageType.MESSAGE_GROUP_MES)
					.content(text)
					.sender(user.getUserID())
					.setUserlist(currentGroupMembers)
					.fontColor(colorHex)
					.bold(currentBold)
					.fontSize(currentFontSize)
					.build();
		} else {
			message = new Message.Builder()
					.mesType(MessageType.MESSAGE_COMM_MES)
					.receiver(chatWith)
					.content(text)
					.sender(user.getUserID())
					.fontColor(colorHex)
					.bold(currentBold)
					.fontSize(currentFontSize)
					.build();
		}

		inputField.setText("");

		// 将自己发送的消息也存入缓存区
		String cacheKey;
		if (isWorldChat) {
			cacheKey = "世界聊天";
		} else if (isGroupChat) {
			List<String> sorted = new ArrayList<>(currentGroupMembers);
			Collections.sort(sorted);
			cacheKey = "群聊:" + String.join(",", sorted);
		} else {
			cacheKey = chatWith;
		}
		manageClientService.addMessageToBuffer(cacheKey, message);

		manageClientService.sendPrivateMessage(message);
	}

	private void appendChat(String sender, String msg, Color color, boolean bold, int fontSize, String sendTimeStr) {
		StyledDocument doc = chatArea.getStyledDocument();
		SimpleAttributeSet attr = new SimpleAttributeSet();
		SimpleAttributeSet timeAttr = new SimpleAttributeSet();
		StyleConstants.setForeground(timeAttr, Color.GRAY);
		StyleConstants.setFontSize(timeAttr, 12);
		StyleConstants.setForeground(attr, color);
		StyleConstants.setBold(attr, bold);
		StyleConstants.setFontSize(attr, fontSize);

		try {
			// 如果是UserID，则尝试解析为 昵称(UserID)
			String displayName = sender;
			if (sender.equals(user.getUserID())) {
				displayName = user.getName() + "(" + user.getUserID() + ")";
			} else if (userIdToNickname.containsKey(sender)) {
				displayName = userIdToNickname.get(sender) + "(" + sender + ")";
			}

			doc.insertString(doc.getLength(), sendTimeStr + " " + displayName + ": " + "\n", timeAttr);
			doc.insertString(doc.getLength(), msg + "\n", attr);
			chatArea.setCaretPosition(doc.getLength());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private void updateOnlineUsers(List<String> users) {
		listModel.clear();
		for (String u : users) {
			listModel.addElement(u);
			// 解析并保存 ID -> 昵称 映射
			String uid = extractUserId(u);
			String nick = extractNickname(u);
			if (uid != null && nick != null) {
				userIdToNickname.put(uid, nick);
				nicknameToUserId.put(nick, uid);
			}
		}
		if (listModel.isEmpty())
			listModel.addElement("暂无在线用户");
	}

	/**
	 * 从缓存加载并显示聊天历史记录
	 * 
	 * @param chatKey 聊天对象标识（用户名或"世界聊天"）
	 */
	private void loadChatHistory(String chatKey) {
		chatArea.setText("");
		List<Message> messages = manageClientService.getMessages(chatKey);
		for (Message msg : messages) {
			Color c = Color.BLACK;
			boolean b = false;
			int size = 14;
			String sendTimeStr = "";
			try {
				Date sendTime = msg.getSendTime();
				SimpleDateFormat sendTimeFormat = new SimpleDateFormat("HH:mm");
				sendTimeStr = sendTimeFormat.format(sendTime);
				c = Color.decode(msg.getFontColor());
				b = msg.isBold();
				size = msg.getFontSize();
			} catch (Exception ignored) {
			}
			appendChat(msg.getSender(), msg.getContent(), c, b, size, sendTimeStr);
		}
	}

	private void showCreateGroupDialog() {
		// 创建用户选择列表（多选）
		DefaultListModel<String> selectModel = new DefaultListModel<>();
		for (int i = 0; i < listModel.size(); i++) {
			String u = listModel.getElementAt(i);
			String userId = extractUserId(u);
			if (!userId.equals(user.getUserID()) && !"暂无在线用户".equals(u)) {
				selectModel.addElement(u);
			}
		}

		if (selectModel.isEmpty()) {
			JOptionPane.showMessageDialog(this, "没有可选择的在线用户", "提示", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		JList<String> selectList = new JList<>(selectModel);
		selectList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		JScrollPane scrollPane = new JScrollPane(selectList);
		scrollPane.setPreferredSize(new Dimension(200, 150));

		int result = JOptionPane.showConfirmDialog(this, scrollPane, "选择群聊成员",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (result == JOptionPane.OK_OPTION) {
			List<String> selectedUsers = selectList.getSelectedValuesList();
			if (selectedUsers.isEmpty()) {
				JOptionPane.showMessageDialog(this, "请至少选择一个用户", "提示", JOptionPane.WARNING_MESSAGE);
				return;
			}

			// 修复：使用UserID而非昵称构建群成员列表
			List<String> groupMemberIds = new ArrayList<>();
			for (String displayName : selectedUsers) {
				groupMemberIds.add(extractUserId(displayName));
			}
			groupMemberIds.add(user.getUserID()); // 加入自己的UserID

			// 设置群聊状态
			currentGroupMembers = new ArrayList<>(groupMemberIds);
			isGroupChat = true;
			isWorldChat = false;

			// 生成群聊标题（用于内部key）
			List<String> sorted = new ArrayList<>(groupMemberIds);
			Collections.sort(sorted);
			chatWith = "群聊:" + String.join(",", sorted);

			((JLabel) ((JPanel) getContentPane().getComponent(1)).getComponent(0))
					.setText("群聊 (" + selectedUsers.size() + "人)");

			// 发送群聊创建消息给服务器，广播给所有群成员
			Message createMsg = new Message.Builder()
					.mesType(MessageType.MESSAGE_GROUP_CREATE)
					.sender(user.getUserID()) // 使用UserID
					.setUserlist(groupMemberIds) // 使用UserID列表
					.build();
			manageClientService.sendPrivateMessage(createMsg);

			loadChatHistory(chatWith);
		}
	}

	private String getUnreadKey(Message msg) {
		String type = msg.getMesType();
		if (MessageType.MESSAGE_WORLD_CHAT.equals(type)) {
			return "世界聊天";
		} else if (MessageType.MESSAGE_COMM_MES.equals(type)) {
			// 修复：使用UserID作为key，而非昵称
			return extractUserId(msg.getSender());
		} else if (MessageType.MESSAGE_GROUP_MES.equals(type)) {
			List<String> members = msg.getUserlist();
			List<String> sorted = new ArrayList<>(members);
			Collections.sort(sorted);
			return "群聊:" + String.join(",", sorted);
		}
		return null;
	}

	private void updateUnreadIndicators() {
		// 刷新用户列表显示
		userList.repaint();
		// 刷新群聊按钮
		for (Map.Entry<String, JButton> entry : groupButtonMap.entrySet()) {
			String groupKey = entry.getKey();
			JButton btn = entry.getValue();
			int count = unreadCounts.getOrDefault(groupKey, 0);
			// 更新按钮文本
			String baseName = btn.getName();
			if (baseName == null)
				baseName = btn.getText().replaceAll(" \\(\\d+\\)$", "");
			if (count > 0) {
				btn.setText(baseName + " (" + count + ")");
				btn.setForeground(Color.RED);
			} else {
				btn.setText(baseName);
				btn.setForeground(Color.BLACK);
			}
		}
		// 世界聊天按钮
		int worldCount = unreadCounts.getOrDefault("世界聊天", 0);
		if (worldCount > 0) {
			worldChatButton.setText("世界聊天 (" + worldCount + ")");
			worldChatButton.setForeground(Color.RED);
		} else {
			worldChatButton.setText("世界聊天");
			worldChatButton.setForeground(Color.BLACK);
		}
	}

	private void clearUnreadAndRefresh(String key) {
		unreadCounts.remove(key);
		updateUnreadIndicators();
	}

	// 用户列表自定义渲染器（带红点）
	private class UnreadCellRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index,
				boolean isSelected, boolean cellHasFocus) {
			JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			String userName = (String) value;
			// 修复：提取UserID后查找未读数
			String userId = extractUserId(userName);
			int count = unreadCounts.getOrDefault(userId, 0);
			if (count > 0) {
				label.setText(userName + " 🔴");
				if (!isSelected) {
					label.setForeground(Color.RED);
				}
			}
			return label;
		}
	}

	/**
	 * 从"昵称(UserID)"格式中提取UserID
	 * 
	 * @param displayName 显示的名字，格式："昵称(UserID)"
	 * @return UserID，如果格式不匹配则返回原字符串
	 */
	private String extractUserId(String displayName) {
		if (displayName == null)
			return "";
		// 匹配格式：昵称(12345678)
		int startIdx = displayName.lastIndexOf('(');
		int endIdx = displayName.lastIndexOf(')');
		if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
			return displayName.substring(startIdx + 1, endIdx);
		}
		// 如果不匹配，直接返回原值（可能只是UserID）
		return displayName;
	}

	/**
	 * 从"昵称(UserID)"格式中提取昵称
	 * 
	 * @param displayName 显示的名字，格式："昵称(UserID)"
	 * @return 昵称，如果格式不匹配则返回原字符串
	 */
	private String extractNickname(String displayName) {
		if (displayName == null)
			return "";
		int startIdx = displayName.lastIndexOf('(');
		if (startIdx != -1) {
			return displayName.substring(0, startIdx);
		}
		return displayName;
	}

	/**
	 * 发送抖动消息
	 */
	private void sendShake() {
		if (isWorldChat) {
			JOptionPane.showMessageDialog(this, "世界聊天不支持抖动", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (isGroupChat) {
			JOptionPane.showMessageDialog(this, "群聊不支持抖动", "提示", JOptionPane.WARNING_MESSAGE);
			return;
		}

		// 发送抖动消息
		Message shakeMsg = new Message.Builder()
				.mesType(MessageType.MESSAGE_SHAKE)
				.sender(user.getUserID())
				.receiver(chatWith)
				.build();
		manageClientService.sendPrivateMessage(shakeMsg);

		// 自己的窗口也抖动一下
		shakeWindow();
	}

	/**
	 * 窗口抖动动画
	 */
	public void shakeWindow() {
		if (!isVisible())
			return;

		Point original = getLocation();
		Timer timer = new Timer(50, null);
		final int[] offsets = { 10, -10, 10, -10, 5, -5, 0 };
		final int[] count = { 0 };

		timer.addActionListener(e -> {
			if (count[0] < offsets.length) {
				setLocation(original.x + offsets[count[0]], original.y);
				count[0]++;
			} else {
				setLocation(original);
				((Timer) e.getSource()).stop();
			}
		});
		timer.start();
	}

	/**
	 * 接收文件并保存
	 */
	private void receiveFile(Message msg) {
		try {
			String fileName = msg.getFileName();
			byte[] fileData = msg.getFileData();

			if (fileName == null || fileData == null) {
				return;
			}

			// 创建downloads目录
			java.io.File downloadsDir = new java.io.File("downloads");
			if (!downloadsDir.exists()) {
				downloadsDir.mkdirs();
			}

			// 保存文件
			java.io.File saveFile = new java.io.File(downloadsDir, fileName);
			java.nio.file.Files.write(saveFile.toPath(), fileData);

			// 在聊天面板显示接收成功
			String successMsg = "[收到文件: " + fileName + " (" + (fileData.length / 1024) + "KB)\n保存到: "
					+ saveFile.getAbsolutePath() + "]";
			appendChat("系统", successMsg, new Color(0, 100, 200), false, 12,
					new java.text.SimpleDateFormat("HH:mm").format(new Date()));
			// 播放文件接收提示音
			SoundManager.playFileSound();

		} catch (Exception e) {
			e.printStackTrace();
			String errorMsg = "[文件接收失败: " + e.getMessage() + "]";
			appendChat("系统", errorMsg, Color.RED, false, 12,
					new java.text.SimpleDateFormat("HH:mm").format(new Date()));
		}
	}
}
