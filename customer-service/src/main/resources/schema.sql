-- 客服系统数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS customer_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE customer_service;

-- 客服账号表
CREATE TABLE IF NOT EXISTS agent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    role TINYINT DEFAULT 1 COMMENT '角色: 1:普通客服 2:管理员',
    status TINYINT DEFAULT 1 COMMENT '在线状态: 1:在线 2:忙碌 3:离线',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服账号表';

-- 访客表
CREATE TABLE IF NOT EXISTS visitor (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '访客用户名',
    password VARCHAR(255) NOT NULL COMMENT '访客密码',
    nickname VARCHAR(100) COMMENT '访客昵称',
    phone VARCHAR(20) COMMENT '手机号',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='访客表';

-- 会话表
CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    visitor_id VARCHAR(64) NOT NULL COMMENT '访客ID',
    visitor_nickname VARCHAR(100) COMMENT '访客昵称',
    visitor_ip VARCHAR(64) COMMENT '访客IP',
    agent_id BIGINT COMMENT '接手客服ID',
    agent_nickname VARCHAR(50) COMMENT '客服昵称',
    status TINYINT DEFAULT 1 COMMENT '会话状态: 1:等待中 2:进行中 3:已结束 4:已评价',
    queue_position INT DEFAULT 0 COMMENT '排队位置',
    started_at TIMESTAMP NULL COMMENT '开始时间',
    ended_at TIMESTAMP NULL COMMENT '结束时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_visitor_id (visitor_id),
    INDEX idx_agent_id (agent_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    conversation_id BIGINT NOT NULL COMMENT '会话ID',
    sender_type TINYINT NOT NULL COMMENT '发送者类型: 1:访客 2:客服 3:系统',
    sender_id VARCHAR(64) NOT NULL COMMENT '发送者ID',
    sender_name VARCHAR(100) COMMENT '发送者名称',
    content TEXT NOT NULL COMMENT '消息内容',
    message_type TINYINT DEFAULT 1 COMMENT '消息类型: 1:文字 2:图片 3:文件 4:表情 5:富文本',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读: 0:未读 1:已读',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_sender_id (sender_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 满意度评价表
CREATE TABLE IF NOT EXISTS rating (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    conversation_id BIGINT NOT NULL UNIQUE COMMENT '会话ID',
    rating TINYINT COMMENT '评分1-5',
    comment TEXT COMMENT '评价内容',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='满意度评价表';

-- 快捷回复语表
CREATE TABLE IF NOT EXISTS quick_reply (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    content TEXT NOT NULL COMMENT '回复内容',
    category VARCHAR(50) COMMENT '分类',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快捷回复语表';

-- 插入默认管理员账号 (密码: admin123)
INSERT INTO agent (username, password, nickname, role, status) VALUES
('admin', 'admin123', '管理员', 2, 1),
('agent1', 'agent123', '客服小明', 1, 1),
('agent2', 'agent123', '客服小红', 1, 1);

-- 插入示例快捷回复
INSERT INTO quick_reply (content, category, sort_order) VALUES
('您好，请问有什么可以帮助您的？', '问候', 1),
('感谢您的咨询，祝您生活愉快！', '结束', 2),
('请稍等，我正在为您查询。', '等待', 3),
('好的，我明白了。', '确认', 4);
