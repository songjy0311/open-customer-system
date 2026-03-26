package com.kefu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kefu.entity.Visitor;

public interface VisitorService extends IService<Visitor> {
    Visitor login(String username, String password);

    Visitor register(String username, String password, String nickname, String phone);
}
