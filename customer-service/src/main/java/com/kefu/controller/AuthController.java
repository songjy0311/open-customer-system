package com.kefu.controller;

import com.kefu.dto.LoginRequest;
import com.kefu.dto.Result;
import com.kefu.entity.Agent;
import com.kefu.entity.Visitor;
import com.kefu.service.AgentService;
import com.kefu.service.VisitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private VisitorService visitorService;

    @PostMapping("/login")
    public Result<?> agentLogin(@RequestBody LoginRequest request) {
        Agent agent = agentService.login(request.getUsername(), request.getPassword());
        if (agent == null) {
            return Result.error("用户名或密码错误");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", agent.getId());
        data.put("username", agent.getUsername());
        data.put("nickname", agent.getNickname());
        data.put("role", agent.getRole());
        data.put("status", agent.getStatus());
        return Result.success(data);
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestParam Long agentId) {
        agentService.updateStatus(agentId, 3); // 3 = 离线
        return Result.success();
    }

    @GetMapping("/status")
    public Result<?> getStatus(@RequestParam Long agentId) {
        Agent agent = agentService.getById(agentId);
        if (agent == null) {
            return Result.error("用户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", agent.getId());
        data.put("username", agent.getUsername());
        data.put("nickname", agent.getNickname());
        data.put("status", agent.getStatus());
        return Result.success(data);
    }

    @PostMapping("/visitor/login")
    public Result<?> visitorLogin(@RequestBody LoginRequest request) {
        Visitor visitor = visitorService.login(request.getUsername(), request.getPassword());
        if (visitor == null) {
            return Result.error("用户名或密码错误");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", visitor.getId());
        data.put("username", visitor.getUsername());
        data.put("nickname", visitor.getNickname());
        return Result.success(data);
    }

    @PostMapping("/visitor/register")
    public Result<?> visitorRegister(@RequestBody LoginRequest request) {
        Visitor visitor = visitorService.register(request.getUsername(), request.getPassword(),
                request.getUsername(), null);
        if (visitor == null) {
            return Result.error("注册失败");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("id", visitor.getId());
        data.put("username", visitor.getUsername());
        data.put("nickname", visitor.getNickname());
        return Result.success(data);
    }
}
