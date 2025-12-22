package client.model;

import common.User;

/**
 * 客户端用户模型类 - 用于本地用户验证和会话管理
 * <p>
 * 【核心作用】
 * 1. 封装当前登录用户信息
 * 2. 提供本地用户验证方法（调试用）
 * <p>
 * 【特殊说明】
 * checkUser方法为硬编码验证，仅用于离线测试
 * 实际登录验证由服务端通过数据库完成
 * 
 * @author ChatRoom Team
 */
public class ClientUser {

	/** 当前用户对象 */
	private User user = new User();

	/**
	 * 本地用户验证方法（硬编码验证）
	 * <p>
	 * 【特殊说明】此方法仅用于离线调试测试
	 * 生产环境中登录验证由服务端完成
	 * 
	 * @param u 待验证的用户对象
	 * @return true表示验证通过
	 */
	public boolean checkUser(User u) {

		return "test".equals(u.getName()) && "123456".equals(u.getPassword());
	}

	/** 获取当前用户对象 */
	public User getUser() {
		return user;
	}

	/** 设置当前用户对象 */
	public void setUser(User user) {
		this.user = user;
	}
}
