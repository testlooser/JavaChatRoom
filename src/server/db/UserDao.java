package server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import common.User;

/**
 * 用户数据访问对象（DAO）- 处理用户相关的数据库操作
 * <p>
 * 【核心作用】
 * 1. 用户登录验证（checkUserById）
 * 2. 用户注册（RegisterUser）
 * 3. 用户信息查询（getUserById）
 * 4. 用户ID唯一性检查（checkIDUnique）
 * <p>
 * 【数据库表结构】
 * users表字段：uid(主键), nickname, password, gender, avatar
 * <p>
 * 【使用方式】
 * 每次操作需要创建新的UserDao实例，使用完毕后调用closeConnection()
 * 
 * <pre>
 * UserDao dao = new UserDao();
 * try {
 * 	boolean exists = dao.checkUserById(userId, password);
 * } finally {
 * 	dao.closeConnection();
 * }
 * </pre>
 * 
 * @author ChatRoom Team
 */
public class UserDao {
	private final Connection conn;
	private PreparedStatement pstmt;
	private ResultSet rs;

	public UserDao() throws SQLException {
		conn = DBUtil.getConnection();
	}

	/**
	 * 通过用户名和密码验证用户（已废弃，保留兼容性）
	 */
	@Deprecated
	public boolean checkUser(String username, String passwd) throws SQLException {
		String sql = "SELECT * FROM users WHERE nickname=? AND password=?";
		pstmt = conn.prepareStatement(sql);
		pstmt.setString(1, username);
		pstmt.setString(2, passwd);
		rs = pstmt.executeQuery();
		return rs.next();
	}

	/**
	 * 注册新用户
	 * 
	 * @param userId   8位用户ID
	 * @param nickname 昵称
	 * @param password 密码
	 * @param gender   性别
	 * @return 是否注册成功
	 */
	public boolean RegisterUser(String userId, String nickname, String password, String gender) throws SQLException {
		String sql = "INSERT INTO users(uid, nickname, password, gender, avatar) VALUES(?, ?, ?, ?, ?)";
		pstmt = conn.prepareStatement(sql);
		pstmt.setString(1, userId);
		pstmt.setString(2, nickname);
		pstmt.setString(3, password);
		pstmt.setString(4, gender);
		pstmt.setInt(5, 1); // 默认头像为1
		int rows = pstmt.executeUpdate();
		return rows > 0;
	}

	/**
	 * 检查用户ID是否唯一（未被使用）
	 */
	public boolean checkIDUnique(String userId) throws SQLException {
		String sql = "SELECT * FROM users WHERE uid=?";
		pstmt = conn.prepareStatement(sql);
		pstmt.setString(1, userId);
		rs = pstmt.executeQuery();
		return !rs.next();
	}

	/**
	 * 通过 ID 和密码验证用户（用于登录）
	 */
	public boolean checkUserById(String userId, String password) throws SQLException {
		String sql = "SELECT * FROM users WHERE uid=? AND password=?";
		pstmt = conn.prepareStatement(sql);
		pstmt.setString(1, userId);
		pstmt.setString(2, password);
		rs = pstmt.executeQuery();
		return rs.next();
	}

	/**
	 * 通过 ID 获取用户信息（返回昵称等）
	 */
	public User getUserById(String userId) throws SQLException {
		String sql = "SELECT * FROM users WHERE uid=?";
		pstmt = conn.prepareStatement(sql);
		pstmt.setString(1, userId);
		rs = pstmt.executeQuery();
		if (rs.next()) {
			User user = new User();
			user.setUserID(rs.getString("uid"));
			user.setNicname(rs.getString("nickname"));
			user.setPassword(rs.getString("password"));
			user.setGender(rs.getString("gender"));
			user.setAvatar(rs.getInt("avatar"));
			return user;
		}
		return null;
	}

	/**
	 * 关闭数据库连接和资源
	 */
	public void closeConnection() throws SQLException {
		try {
			if (rs != null)
				rs.close();
			if (pstmt != null)
				pstmt.close();
			if (conn != null)
				conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}
}
