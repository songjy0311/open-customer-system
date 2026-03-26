package com.kefu.controller;

import com.kefu.dto.Result;
import com.kefu.entity.Message;
import com.kefu.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/history/{conversationId}")
    public Result<List<Message>> getHistoryMessages(@PathVariable Long conversationId) {
        List<Message> messages = messageService.getHistoryMessages(conversationId);
        return Result.success(messages);
    }

    @GetMapping("/unread/{conversationId}")
    public Result<Long> getUnreadCount(@PathVariable Long conversationId) {
        long count = messageService.getUnreadCount(conversationId);
        return Result.success(count);
    }

    @PostMapping("/read")
    public Result<?> markAsRead(@RequestParam Long conversationId, @RequestParam String receiverId) {
        messageService.markAsRead(conversationId, receiverId);
        return Result.success();
    }
}
