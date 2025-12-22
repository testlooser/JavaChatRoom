package server.service;

import common.User;

/**
 * 用户服务类 - 提供用户验证功能（已废弃）
 * <p>
 * 【注意】此类已被废弃，实际验证逻辑已迁移到UserDao
 * 此类仅用于向后兼容和硬编码测试
 * <p>
 * 【硬编码账号】
 * - admin / 123456
 * 
 * @author ChatRoom Team
 * @deprecated 使用 {@link server.db.UserDao#checkUserById} 替代
 */
@Deprecated
public class UserService {
	/** 用户对象（未使用） */
	User user = null;

	/**
	 * 默认构造器
	 */
	public UserService() {
	}

	/**
	 * 验证用户（硬编码验证）
	 * 
	 * @param userId 用户ID
	 * @param pwd    密码
	 * @return 验证结果（目前始终返回true或匹配admin账号）
	 * @deprecated 使用 {@link server.db.UserDao#checkUserById} 替代
	 */
	@Deprecated
	public static boolean checkUser(String userId, String pwd) {
		if (userId.equals("admin") && pwd.equals("123456")) {
			return true;
		}
		// 从数据库查询用户
		// return userDao.checkUser(userId, pwd);
		return true;
	}
}
