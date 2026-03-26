package com.kefu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rating")
public class Rating {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID
     */
    private Long conversationId;

    /**
     * 评分1-5
     */
    private Integer rating;

    /**
     * 评价内容
     */
    private String comment;

    private LocalDateTime createdAt;
}
