package com.kefu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("visitor")
public class Visitor {
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 访客用户名
     */
    private String username;

    /**
     * 访客密码
     */
    private String password;

    /**
     * 访客昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
