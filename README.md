# 🚀 Java 局域网聊天室系统

<div align="center">

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=for-the-badge&logo=mysql)
![Socket](https://img.shields.io/badge/Socket-TCP-green?style=for-the-badge)
![Swing](https://img.shields.io/badge/GUI-Swing-purple?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

**一个基于 Java Socket 的局域网即时通讯系统，采用 C/S 架构设计**

[功能特性](#-功能特性) • [快速开始](#-快速开始) • [项目结构](#-项目结构) • [技术亮点](#-技术亮点) • [使用说明](#-使用说明)

</div>

---

## 📖 项目简介

本项目是一个基于 **Java Socket (TCP)** 的局域网聊天室系统，作为计算机网络课程的结课项目开发。系统采用经典的 **C/S（客户端/服务器）架构**，实现了多用户实时通信功能，重点展示了 TCP 协议在即时通讯场景中的应用。

### 🎯 核心技术栈

| 技术领域 | 技术选型                |
| -------- | ----------------------- |
| 编程语言 | Java 17                 |
| 网络通信 | Java Socket (TCP)       |
| 数据库   | MySQL 8.0 + c3p0 连接池 |
| 界面框架 | Java Swing              |
| 数据传输 | Java 对象序列化         |

---

## ✨ 功能特性

### 💬 聊天功能

- ✅ **私聊消息** - 一对一私密聊天
- ✅ **群聊消息** - 多人群组聊天
- ✅ **世界聊天** - 全服广播消息
- ✅ **富文本支持** - 自定义字体颜色、大小、粗体
- ✅ **聊天记录** - 本地持久化存储，重启后可恢复

### 📁 文件传输

- ✅ **文件发送** - 支持发送任意类型文件（限 5MB）
- ✅ **截图发送** - 框选屏幕区域发送截图
- ✅ **文件接收** - 自动保存到 downloads 目录

### 👥 用户管理

- ✅ **用户注册** - 自动生成 8 位唯一用户 ID
- ✅ **用户登录** - 用户 ID + 密码验证
- ✅ **在线列表** - 实时显示在线用户

### 🎨 特色功能

- ✅ **窗口抖动** - QQ 式抖一抖效果
- ✅ **消息提示音** - 新消息到达提示
- ✅ **系统托盘** - 最小化到托盘，图标闪烁提醒
- ✅ **离线消息** - 用户离线时消息暂存，上线后推送
- ✅ **未读计数** - 各聊天窗口未读消息计数显示
- ✅ **服务端 GUI** - 可视化监控面板，支持系统广播

---

## 🚀 快速开始

### 环境要求

- **JDK 17** 或更高版本
- **MySQL 8.0** 数据库
- **IntelliJ IDEA** 或其他 Java IDE（推荐）

### 安装步骤

#### 1️⃣ 克隆项目

```bash
git clone https://github.com/yourusername/JAVA_INTERNET_ChatRoom.git
cd JAVA_INTERNET_ChatRoom
```

#### 2️⃣ 配置数据库

1. 创建 MySQL 数据库：

```sql
CREATE DATABASE qq_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 导入数据库表结构（执行项目根目录下的 `qq_db.sql`）：

```bash
mysql -u root -p qq_db < qq_db.sql
```

3. 修改数据库连接配置 `src/c3p0-config.xml`：

```xml
<property name="jdbcUrl">jdbc:mysql://localhost:3306/qq_db?useSSL=false&serverTimezone=UTC</property>
<property name="user">root</property>
<property name="password">你的数据库密码</property>
```

#### 3️⃣ 导入依赖

将 `lib/` 目录下的 JAR 包添加到项目依赖：

- `c3p0-0.9.5.5.jar` - 数据库连接池
- `mchange-commons-java-0.2.19.jar` - c3p0 依赖
- `mysql-connector-j-8.0.33.jar` - MySQL 驱动

**IntelliJ IDEA 操作：**

> File → Project Structure → Libraries → + → Java → 选择 lib 目录下的所有 jar 文件

#### 4️⃣ 运行项目

**启动服务端：**

```bash
# 运行 server.ServerMain
java -cp "out/production/JAVA_INTERNET_ChatRoom;lib/*" server.ServerMain
```

**启动客户端：**

```bash
# 运行 client.ClientMain（可启动多个实例）
java -cp "out/production/JAVA_INTERNET_ChatRoom;lib/*" client.ClientMain
```

---

## 📁 项目结构

```
JAVA_INTERNET_ChatRoom/
├── src/
│   ├── common/                    # 公共模块（客户端和服务端共用）
│   │   ├── Message.java           # 消息实体类（Builder模式）
│   │   ├── MessageType.java       # 消息类型常量接口
│   │   └── User.java              # 用户实体类
│   │
│   ├── server/                    # 服务端模块
│   │   ├── ServerMain.java        # 服务端主入口
│   │   ├── db/
│   │   │   ├── DBUtil.java        # 数据库连接池工具
│   │   │   └── UserDao.java       # 用户数据访问层
│   │   ├── service/
│   │   │   ├── ManageClientThread.java        # 客户端线程管理器
│   │   │   └── ServerConnectClientThread.java # 消息处理线程
│   │   └── view/
│   │       └── ServerGUI.java     # 服务端监控界面
│   │
│   ├── client/                    # 客户端模块
│   │   ├── ClientMain.java        # 客户端主入口
│   │   ├── model/
│   │   │   ├── ChatHistoryManager.java # 聊天记录管理器
│   │   │   └── ClientUser.java         # 客户端用户模型
│   │   ├── service/
│   │   │   ├── ClientConnectServer.java   # 网络通信线程
│   │   │   └── ManageClientService.java   # 业务逻辑处理
│   │   ├── utils/
│   │   │   ├── ScreenshotUtil.java  # 截图工具
│   │   │   ├── SoundManager.java    # 音效管理
│   │   │   └── TrayManager.java     # 系统托盘管理
│   │   └── view/
│   │       ├── LoginView.java       # 登录界面
│   │       ├── RegisterDialog.java  # 注册对话框
│   │       └── MainChatView.java    # 主聊天界面
│   │
│   └── c3p0-config.xml            # 数据库连接池配置
│
├── lib/                           # 第三方依赖库
├── resources/                     # 资源文件
│   └── sounds/                    # 音效资源
├── chathistory/                   # 聊天记录存储目录
├── downloads/                     # 文件下载目录
├── docs/                          # 项目文档
└── qq_db.sql                      # 数据库初始化脚本
```

---

## 🔧 技术亮点

### 1. Builder 模式构建消息

```java
Message msg = new Message.Builder()
    .mesType(MessageType.MESSAGE_COMM_MES)
    .sender("12345678")
    .receiver("87654321")
    .content("Hello!")
    .fontColor("#FF0000")
    .bold(true)
    .fontSize(16)
    .build();
```

**优势：** 避免构造函数参数过多，链式调用更加清晰易读。

### 2. 观察者模式解耦通信层和 UI 层

```java
// 通信线程通过监听器通知UI更新
public interface PrivateMessageListener {
    void onMessageReceived(Message msg);
}

// MainChatView 注册监听器
ClientConnectServer.getInstance().addPrivateMessageListener(msg -> {
    SwingUtilities.invokeLater(() -> displayMessage(msg));
});
```

**优势：** 业务逻辑与界面展示完全解耦，便于扩展和维护。

### 3. 线程安全的消息缓存

```java
// 使用 ConcurrentHashMap 保证线程安全
private static final ConcurrentHashMap<String, ServerConnectClientThread> clientThreads;
```

**优势：** 多线程环境下安全管理用户线程映射。

### 4. 离线消息机制

```java
if (receiverThread != null) {
    receiverThread.getOOS().writeObject(msg);  // 在线直接转发
} else {
    offlineMessages.computeIfAbsent(receiverId, k -> new ArrayList<>()).add(msg);  // 离线暂存
}
```

**优势：** 保证消息不丢失，用户上线后自动推送。

### 5. 消息缓存 + 文件持久化

```java
// 内存缓存加速读取
messageBuffer.put(key, messages);

// 文件序列化保证记录不丢失
historyManager.saveMessages(key, messages);
```

**优势：** 双重保障聊天记录安全，重启应用后可恢复历史消息。

---

## 📖 使用说明

### 基本操作

| 操作        | 说明                                 |
| ----------- | ------------------------------------ |
| 🔐 登录     | 输入用户 ID 和密码，点击登录         |
| 📝 注册     | 点击注册按钮，填写昵称、密码、性别   |
| 💬 私聊     | 双击左侧在线用户列表中的用户         |
| 🌍 世界聊天 | 点击"世界聊天"按钮，消息广播给所有人 |
| 👥 创建群聊 | 点击"新建群聊"，选择成员后确认       |
| 📎 发送文件 | 私聊界面点击"发送文件"按钮           |
| 📸 发送截图 | 点击"截图"按钮，框选区域             |
| 👊 窗口抖动 | 点击"抖一抖"按钮                     |

### 服务端监控

服务端启动后会显示监控面板，功能包括：

- 📊 查看在线用户列表
- 📝 查看消息日志
- 📢 发送系统广播

---

## 🌐 网络架构

```
┌──────────────┐                    ┌──────────────┐
│   客户端A    │                    │   客户端B    │
│  ┌────────┐  │                    │  ┌────────┐  │
│  │ Socket │  │                    │  │ Socket │  │
│  └───┬────┘  │                    │  └───┬────┘  │
└──────┼───────┘                    └──────┼───────┘
       │         TCP连接                   │
       │     ┌─────────────────┐          │
       └────►│   服务器        │◄─────────┘
             │  ServerSocket   │
             │    (8888)       │
             │  ┌───────────┐  │
             │  │线程管理器 │  │
             └─────────────────┘
```

---

## 📋 数据库设计

### users 表结构

```sql
CREATE TABLE users (
    uid VARCHAR(8) PRIMARY KEY COMMENT '8位用户ID',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    password VARCHAR(50) NOT NULL COMMENT '密码',
    gender VARCHAR(10) DEFAULT '保密' COMMENT '性别',
);
```

---

## ❓ 常见问题

### Q: 服务端启动报错 "ComboPooledDataSource cannot be resolved"？

**A:** 需要将 `lib/` 目录下的 JAR 包添加到项目依赖中。详见[安装步骤](#3%EF%B8%8F⃣-导入依赖)。

### Q: 客户端无法连接服务器？

**A:**

1. 确保服务端已启动
2. 检查防火墙是否放行 8888 端口
3. 如果是跨机器访问，需要修改 `ClientMain.java` 中的 `ip` 变量为服务器实际 IP

### Q: 如何支持广域网访问？

**A:** 本项目为局域网设计，如需广域网访问：

1. 在路由器上配置端口映射（8888 端口）
2. 使用内网穿透工具（如 frp、ngrok）
3. 将服务端部署到云服务器

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建新分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

## 📄 开源协议

本项目采用 [MIT License](LICENSE) 开源协议。

<div align="center">

**如果这个项目对你有帮助，请给个 ⭐ Star 支持一下！**

Made with ❤️ by ChatRoom Team

</div>
