package com.kefu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 发送者类型: 1:访客 2:客服 3:系统
     */
    private Integer senderType;

    /**
     * 发送者ID
     */
    private String senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型: 1:文字 2:图片 3:文件 4:表情 5:富文本
     */
    private Integer messageType;

    /**
     * 是否已读: 0:未读 1:已读
     */
    private Integer isRead;

    private LocalDateTime createdAt;
}
