package common;

import java.io.Serializable;

/**
 * 用户实体类 - 聊天系统的核心数据模型
 * <p>
 * 【核心作用】承载用户的身份信息，用于：
 * 1. 用户登录验证（UserID + password）
 * 2. 用户注册信息存储
 * 3. 在客户端和服务端之间传输用户数据
 * <p>
 * 【特殊说明】实现Serializable接口，支持Socket网络传输序列化
 * 
 * @author ChatRoom Team
 */
public class User implements Serializable {
	/** 用户唯一标识ID（8位数字），用于登录验证和消息路由 */
	private String UserID;

	/** 用户名/显示名称 */
	private String name;

	/** 用户密码（明文存储，生产环境应加密） */
	private String password;

	/** 用户昵称，用于界面显示 */
	private String nicname;

	/** 用户性别（男/女/保密） */
	private String gender;

	/** 头像编号，对应头像资源索引 */
	private int avatar;

	/**
	 * 完整构造器 - 用于用户注册时创建完整用户对象
	 * 
	 * @param name     用户名
	 * @param password 密码
	 * @param nicname  昵称
	 * @param gender   性别
	 * @param avatar   头像编号
	 */
	public User(String name, String password, String nicname, String gender, int avatar) {
		this.name = name;
		this.password = password;
		this.nicname = nicname;
		this.gender = gender;
		this.avatar = avatar;
	}

	/**
	 * 简化构造器 - 用于登录验证场景
	 * 
	 * @param clientUser1 用户名/用户ID
	 * @param number      密码
	 */
	public User(String clientUser1, String number) {
		this.name = clientUser1;
		this.password = number;
	}

	/**
	 * 无参构造器 - 用于反序列化和空对象创建
	 */
	public User() {

	}

	/**
	 * 获取用户唯一ID
	 * 【核心方法】用于消息发送、接收的用户标识
	 */
	public String getUserID() {
		return UserID;
	}

	/**
	 * 设置用户唯一ID
	 * 【核心方法】注册成功后由服务器分配
	 */
	public void setUserID(String userID) {
		UserID = userID;
	}

	/** 获取用户名 */
	public String getName() {
		return name;
	}

	/** 设置用户名 */
	public void setName(String name) {
		this.name = name;
	}

	/** 获取密码 */
	public String getPassword() {
		return password;
	}

	/** 设置密码 */
	public void setPassword(String password) {
		this.password = password;
	}

	/** 获取昵称 - 用于界面显示 */
	public String getNicname() {
		return nicname;
	}

	/** 设置昵称 */
	public void setNicname(String nicname) {
		this.nicname = nicname;
	}

	/** 获取性别 */
	public String getGender() {
		return gender;
	}

	/** 设置性别 */
	public void setGender(String gender) {
		this.gender = gender;
	}

	/** 获取头像编号 */
	public int getAvatar() {
		return avatar;
	}

	/** 设置头像编号 */
	public void setAvatar(int avatar) {
		this.avatar = avatar;
	}
}
