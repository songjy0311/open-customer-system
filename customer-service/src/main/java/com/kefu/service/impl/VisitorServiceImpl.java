package com.kefu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kefu.entity.Visitor;
import com.kefu.mapper.VisitorMapper;
import com.kefu.service.VisitorService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class VisitorServiceImpl extends ServiceImpl<VisitorMapper, Visitor> implements VisitorService {

    @Override
    public Visitor login(String username, String password) {
        return this.getOne(new LambdaQueryWrapper<Visitor>()
                .eq(Visitor::getUsername, username)
                .eq(Visitor::getPassword, password));
    }

    @Override
    public Visitor register(String username, String password, String nickname, String phone) {
        Visitor visitor = new Visitor();
        visitor.setUsername(username);
        visitor.setPassword(password);
        visitor.setNickname(nickname);
        visitor.setPhone(phone);
        visitor.setCreatedAt(LocalDateTime.now());
        visitor.setUpdatedAt(LocalDateTime.now());
        this.save(visitor);
        return visitor;
    }
}
