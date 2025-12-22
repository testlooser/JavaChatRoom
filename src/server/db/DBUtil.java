package server.db;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.sql.*;
import javax.sql.DataSource;

/**
 * 数据库工具类 - 管理数据库连接池
 * <p>
 * 【核心作用】
 * 1. 初始化c3p0数据库连接池
 * 2. 提供获取数据库连接的方法
 * 3. 提供关闭连接和释放资源的工具方法
 * <p>
 * 【技术实现】
 * - 使用c3p0连接池管理MySQL连接
 * - 配置文件：src/c3p0-config.xml
 * - 连接池参数：初始5个连接，最大20个连接
 * <p>
 * 【设计说明】
 * 采用连接池而非直接创建连接的优势：
 * - 避免频繁创建/销毁连接的开销
 * - 连接复用提高性能
 * - 池化管理防止连接泄露
 * 
 * @author ChatRoom Team
 */
public class DBUtil {

	/** c3p0数据源实例（全局唯一）*/
	private static DataSource dataSource = null;

	/*
	 * 静态初始化块 - 类加载时自动执行
	 * 从classpath读取c3p0-config.xml配置文件初始化连接池
	 */
	static {
		// 自动读取 src/c3p0-config.xml
		dataSource = new ComboPooledDataSource();
	}

	/**
	 * 从连接池获取数据库连接
	 * 【重要】使用完毕后必须调用closeConnection归还连接
	 * 
	 * @return 数据库连接对象
	 * @throws SQLException 获取连接失败时抛出
	 */
	public static Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	/**
	 * 关闭数据库资源
	 * 【重要】按ResultSet -> Statement -> Connection的顺序关闭
	 * 对于连接池，close()实际上是归还连接而非真正关闭
	 * 
	 * @param conn 数据库连接
	 * @param stmt SQL语句对象
	 * @param rs   结果集
	 */
	public static void closeConnection(Connection conn, Statement stmt, ResultSet rs) {
		try {
			if (rs != null)
				rs.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close(); // 归还连接给连接池
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
