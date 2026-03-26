package com.kefu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("quick_reply")
public class QuickReply {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 回复内容
     */
    private String content;

    /**
     * 分类
     */
    private String category;

    /**
     * 排序
     */
    private Integer sortOrder;

    private LocalDateTime createdAt;
}
