SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `qq_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `qq_db`;

-- 2. 删除并重新创建用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `uid` varchar(20) NOT NULL,
  `password` varchar(20) NOT NULL,
  `nickname` varchar(20) DEFAULT NULL,
  `avatar` int(11) DEFAULT 1,
  `gender` varchar(2) DEFAULT '男',
  PRIMARY KEY (`uid`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci;

-- 3. 插入初始测试数据
INSERT INTO `users` VALUES ('1', '1', 'test1', 1, '男');
INSERT INTO `users` VALUES ('10000000', 'admin', '测1', 1, '男');
INSERT INTO `users` VALUES ('10000001', 'admin', '测2', 1, '男');
INSERT INTO `users` VALUES ('10000002', 'admin', '测3', 1, '男');
INSERT INTO `users` VALUES ('1001', '123', '张三', 1, '男');
INSERT INTO `users` VALUES ('1002', '123', '李四', 2, '女');
INSERT INTO `users` VALUES ('1003', '123', '王五', 3, '男');
INSERT INTO `users` VALUES ('2', '1', 'test2', 1, '男');
INSERT INTO `users` VALUES ('3', '1', 'test3', 1, '男');
INSERT INTO `users` VALUES ('91457969', '123', '叶子', 1, '男');
INSERT INTO `users` VALUES ('97485178', '123', '她她她', 1, '男');

SET FOREIGN_KEY_CHECKS = 1;