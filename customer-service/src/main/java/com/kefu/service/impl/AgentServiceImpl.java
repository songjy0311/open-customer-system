package com.kefu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kefu.entity.Agent;
import com.kefu.enums.AgentStatus;
import com.kefu.mapper.AgentMapper;
import com.kefu.service.AgentService;
import org.springframework.stereotype.Service;

@Service
public class AgentServiceImpl extends ServiceImpl<AgentMapper, Agent> implements AgentService {

    @Override
    public Agent login(String username, String password) {
        Agent agent = this.getOne(new LambdaQueryWrapper<Agent>()
                .eq(Agent::getUsername, username)
                .eq(Agent::getPassword, password));
        if (agent != null) {
            // 登录成功设置为在线状态
            agent.setStatus(AgentStatus.ONLINE);
            this.updateById(agent);
        }
        return agent;
    }

    @Override
    public void updateStatus(Long agentId, Integer status) {
        Agent agent = new Agent();
        agent.setId(agentId);
        agent.setStatus(status);
        this.updateById(agent);
    }
}
