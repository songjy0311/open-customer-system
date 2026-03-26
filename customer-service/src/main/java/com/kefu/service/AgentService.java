package com.kefu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kefu.entity.Agent;

public interface AgentService extends IService<Agent> {
    Agent login(String username, String password);

    void updateStatus(Long agentId, Integer status);
}
