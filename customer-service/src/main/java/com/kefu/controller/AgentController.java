package com.kefu.controller;

import com.kefu.dto.AgentDTO;
import com.kefu.dto.Result;
import com.kefu.entity.Agent;
import com.kefu.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @GetMapping("/list")
    public Result<?> getAgentList() {
        return Result.success(agentService.list());
    }

    @GetMapping("/online")
    public Result<?> getOnlineAgents() {
        return Result.success(agentService.list());
    }

    @PutMapping("/status")
    public Result<?> updateStatus(@RequestParam Long agentId, @RequestParam Integer status) {
        agentService.updateStatus(agentId, status);
        return Result.success();
    }
}
