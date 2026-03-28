package com.kefu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kefu.entity.Message;

import java.util.List;

public interface MessageService extends IService<Message> {
    /**
     * 获取会话历史消息
     */
    List<Message> getHistoryMessages(Long conversationId);

    /**
     * 获取未读消息数
     */
    long getUnreadCount(Long conversationId);

    /**
     * 发送消息
     */
    Message sendMessage(Long conversationId, Integer senderType, String senderId, String senderName,
                        String content, Integer messageType);

    List<Long> markAsRead(Long conversationId, String receiverId);
}
