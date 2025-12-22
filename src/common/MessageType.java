package common;

/**
 * 消息类型常量接口 - 定义客户端与服务器通信的所有消息类型
 * <p>
 * 【核心作用】作为通信协议的类型标识，用于：
 * 1. 客户端构建请求消息时指定类型
 * 2. 服务端解析消息后进行分发处理
 * 3. 双向通信的协议约定
 * <p>
 * 【设计说明】使用接口+常量的方式，便于全局引用
 * 
 * @author ChatRoom Team
 */
public interface MessageType {
	// ==================== 登录认证相关 ====================
	/** 客户端登录请求 */
	String MESSAGE_LOGIN = "请求登录";
	/** 服务端返回登录成功（content携带昵称） */
	String MESSAGE_LOGIN_SUCCEED = "登录成功";
	/** 服务端返回登录失败 */
	String MESSAGE_LOGIN_FAIL = "登录失败";

	// ==================== 用户注册相关 ====================
	/** 客户端注册请求（content格式："昵称|密码|性别"） */
	String MESSAGE_REGISTER = "请求注册";
	/** 服务端返回注册成功（content携带生成的UserID） */
	String MESSAGE_REGISTER_SUCCEED = "注册成功";
	/** 服务端返回注册失败 */
	String MESSAGE_REGISTER_FAIL = "注册失败";

	// ==================== 聊天消息相关 ====================
	/** 私聊普通文字消息 */
	String MESSAGE_COMM_MES = "普通文字";
	/** 世界聊天消息（广播给所有在线用户） */
	String MESSAGE_WORLD_CHAT = "世界聊天";
	/** 群聊消息（发送给指定群成员） */
	String MESSAGE_GROUP_MES = "群聊消息";
	/** 创建群聊通知（广播给所有群成员） */
	String MESSAGE_GROUP_CREATE = "创建群聊";

	// ==================== 在线状态相关 ====================
	/** 客户端请求获取在线用户列表 */
	String MESSAGE_GET_ONLINE_FRIEND = "获取在线列表";
	/** 服务端返回在线用户列表（userlist携带用户列表） */
	String MESSAGE_RET_ONLINE_FRIEND = "返回在线列表";
	/** 客户端退出请求 */
	String MESSAGE_CLIENT_EXIT = "请求退出";

	// ==================== 特殊功能相关 ====================
	/** 窗口抖动消息（仅私聊支持） */
	String MESSAGE_SHAKE = "窗口抖动";
	/** 文件传输消息（携带fileName和fileData） */
	String MESSAGE_FILE = "文件传输";
	/** 服务器系统广播消息 */
	String MESSAGE_SYSTEM_BROADCAST = "系统广播";
}
