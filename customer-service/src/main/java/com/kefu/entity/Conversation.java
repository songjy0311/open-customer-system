package com.kefu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation")
public class Conversation {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 访客ID
     */
    private String visitorId;

    /**
     * 访客昵称
     */
    private String visitorNickname;

    /**
     * 访客IP
     */
    private String visitorIp;

    /**
     * 接手客服ID
     */
    private Long agentId;

    /**
     * 客服昵称
     */
    private String agentNickname;

    /**
     * 会话状态: 1:等待中 2:进行中 3:已结束 4:已评价
     */
    private Integer status;

    /**
     * 排队位置
     */
    private Integer queuePosition;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    private LocalDateTime endedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
