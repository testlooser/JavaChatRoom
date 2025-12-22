package common;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 消息实体类 - 客户端与服务端通信的核心数据载体
 * <p>
 * 【核心作用】封装所有类型的通信消息，包括：
 * - 登录/注册请求与响应
 * - 私聊/群聊/世界聊天消息
 * - 文件传输、窗口抖动等特殊消息
 * <p>
 * 【设计模式】使用Builder模式构建不可变对象：
 * - 所有字段都是final的，保证线程安全
 * - 通过Builder类提供链式调用API
 * - 支持序列化用于Socket网络传输
 * <p>
 * 【使用示例】
 * 
 * <pre>
 * Message msg = new Message.Builder()
 * 		.mesType(MessageType.MESSAGE_COMM_MES)
 * 		.sender("12345678")
 * 		.receiver("87654321")
 * 		.content("Hello!")
 * 		.build();
 * </pre>
 * 
 * @author ChatRoom Team
 */
public class Message implements Serializable, MessageType {

	/** 序列化版本号，保证反序列化兼容性 */
	private static final long serialVersionUID = 1L;

	// ==================== 基础消息字段 ====================
	/** 发送者UserID */
	private final String sender;
	/** 接收者UserID（私聊时使用） */
	private final String receiver;
	/** 消息文本内容 */
	private final String content;
	/** 消息发送时间 */
	private final Date sendTime;
	/** 消息类型，参见MessageType接口常量 */
	private final String mesType;
	/** 在线用户列表/群成员列表 */
	private final List<String> userlist;

	// ==================== 富文本样式字段 ====================
	/** 文字颜色（十六进制格式，如 "#FF0000"） */
	private final String fontColor;
	/** 是否加粗 */
	private final boolean bold;
	/** 字体大小 */
	private final int fontSize;

	// ==================== 文件传输字段 ====================
	/** 文件名（文件传输时使用） */
	private final String fileName;
	/** 文件二进制数据（文件传输时使用） */
	private final byte[] fileData;

	/**
	 * 私有构造器 - 只能通过Builder创建实例
	 * 【设计说明】保证对象不可变性，所有字段通过Builder一次性设置
	 * 
	 * @param builder Builder实例
	 */
	private Message(Builder builder) {
		this.sender = builder.sender;
		this.receiver = builder.receiver;
		this.content = builder.content;
		this.sendTime = builder.sendTime;
		this.mesType = builder.mesType;
		this.userlist = builder.userlist;

		this.fontColor = builder.fontColor;
		this.bold = builder.bold;
		this.fontSize = builder.fontSize;

		this.fileName = builder.fileName;
		this.fileData = builder.fileData;
	}

	// ==================== 基础字段Getter ====================

	/** 获取发送者UserID */
	public String getSender() {
		return sender;
	}

	/** 获取接收者UserID */
	public String getReceiver() {
		return receiver;
	}

	/** 获取消息文本内容 */
	public String getContent() {
		return content;
	}

	/** 获取发送时间 */
	public Date getSendTime() {
		return sendTime;
	}

	/**
	 * 获取消息类型
	 * 【核心方法】用于服务端/客户端消息分发处理
	 */
	public String getMesType() {
		return mesType;
	}

	/** 获取用户列表（在线列表或群成员列表） */
	public List<String> getUserlist() {
		return userlist;
	}

	// ==================== 富文本样式Getter ====================

	/** 获取字体颜色 */
	public String getFontColor() {
		return fontColor;
	}

	/** 是否加粗 */
	public boolean isBold() {
		return bold;
	}

	/** 获取字体大小 */
	public int getFontSize() {
		return fontSize;
	}

	// ==================== 文件传输Getter ====================

	/** 获取文件名 */
	public String getFileName() {
		return fileName;
	}

	/** 获取文件二进制数据 */
	public byte[] getFileData() {
		return fileData;
	}

	@Override
	public String toString() {
		return "Message{" +
				"sender='" + sender + '\'' +
				", receiver='" + receiver + '\'' +
				", content='" + content + '\'' +
				", sendTime='" + sendTime + '\'' +
				", mesType='" + mesType + '\'' +
				", fontColor='" + fontColor + '\'' +
				", bold=" + bold +
				", fontSize=" + fontSize +
				'}';
	}

	/**
	 * Message构建器 - 使用链式调用构建消息对象
	 * <p>
	 * 【设计说明】
	 * - 提供所有字段的默认值，保证向后兼容
	 * - 支持链式调用，代码更简洁
	 * - 在build()时进行参数校验
	 */
	public static class Builder {

		// 基础字段默认值
		private String sender = "default sender";
		private String receiver = "default receiver";
		private String content = "";
		private Date sendTime = new Date(); // 默认当前时间
		private String mesType = MessageType.MESSAGE_LOGIN;
		private List<String> userlist = Collections.singletonList("default");

		// 富文本样式默认值（保证兼容旧代码）
		private String fontColor = "#000000"; // 默认黑色
		private boolean bold = false;
		private int fontSize = 14;

		// 文件传输默认值
		private String fileName = null;
		private byte[] fileData = null;

		/** 设置发送者UserID */
		public Builder sender(String sender) {
			this.sender = sender;
			return this;
		}

		/** 设置接收者UserID */
		public Builder receiver(String receiver) {
			this.receiver = receiver;
			return this;
		}

		/** 设置消息文本内容 */
		public Builder content(String content) {
			this.content = content;
			return this;
		}

		/** 设置发送时间 */
		public Builder sendTime(Date sendTime) {
			this.sendTime = sendTime;
			return this;
		}

		/**
		 * 设置消息类型
		 * 【核心方法】必须设置，否则build()会抛异常
		 */
		public Builder mesType(String mesType) {
			this.mesType = mesType;
			return this;
		}

		/** 设置用户列表（群成员/在线用户） */
		public Builder setUserlist(List<String> userlist) {
			this.userlist = userlist;
			return this;
		}

		/** 设置字体颜色（十六进制格式） */
		public Builder fontColor(String fontColor) {
			this.fontColor = fontColor;
			return this;
		}

		/** 设置是否加粗 */
		public Builder bold(boolean bold) {
			this.bold = bold;
			return this;
		}

		/** 设置字体大小 */
		public Builder fontSize(int fontSize) {
			this.fontSize = fontSize;
			return this;
		}

		/** 设置文件名（文件传输时使用） */
		public Builder fileName(String fileName) {
			this.fileName = fileName;
			return this;
		}

		/** 设置文件数据（文件传输时使用） */
		public Builder fileData(byte[] fileData) {
			this.fileData = fileData;
			return this;
		}

		/**
		 * 构建Message对象
		 * 【核心方法】校验必要字段后创建不可变Message实例
		 * 
		 * @return 构建完成的Message对象
		 * @throws IllegalStateException 如果mesType为null
		 */
		public Message build() {
			if (mesType == null) {
				throw new IllegalStateException("Message type cannot be null");
			}
			return new Message(this);
		}
	}
}
