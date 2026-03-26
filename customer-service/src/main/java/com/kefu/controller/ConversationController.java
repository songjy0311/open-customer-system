package com.kefu.controller;

import com.kefu.dto.ConversationDTO;
import com.kefu.dto.Result;
import com.kefu.entity.Conversation;
import com.kefu.service.ConversationService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/conversation")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @GetMapping("/queue")
    public Result<List<ConversationDTO>> getQueueList() {
        List<Conversation> conversations = conversationService.getQueueList();
        List<ConversationDTO> dtoList = conversations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    @GetMapping("/active")
    public Result<List<ConversationDTO>> getActiveConversations(@RequestParam Long agentId) {
        List<Conversation> conversations = conversationService.getActiveConversations(agentId);
        List<ConversationDTO> dtoList = conversations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return Result.success(dtoList);
    }

    @PostMapping("/{id}/take")
    public Result<?> takeConversation(@PathVariable Long id, @RequestParam Long agentId,
                                        @RequestParam String agentNickname) {
        boolean success = conversationService.takeConversation(id, agentId, agentNickname);
        return success ? Result.success() : Result.error("接手会话失败");
    }

    @PostMapping("/{id}/transfer")
    public Result<?> transferConversation(@PathVariable Long id, @RequestParam Long newAgentId) {
        boolean success = conversationService.transferConversation(id, newAgentId);
        return success ? Result.success() : Result.error("转接会话失败");
    }

    @PostMapping("/{id}/end")
    public Result<?> endConversation(@PathVariable Long id) {
        boolean success = conversationService.endConversation(id);
        return success ? Result.success() : Result.error("结束会话失败");
    }

    private ConversationDTO convertToDTO(Conversation conversation) {
        ConversationDTO dto = new ConversationDTO();
        BeanUtils.copyProperties(conversation, dto);
        return dto;
    }
}
