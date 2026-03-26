package com.kefu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kefu.entity.Message;
import com.kefu.mapper.MessageMapper;
import com.kefu.service.MessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements MessageService {

    @Override
    public List<Message> getHistoryMessages(Long conversationId) {
        return this.list(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .orderByAsc(Message::getCreatedAt));
    }

    @Override
    public long getUnreadCount(Long conversationId) {
        return this.count(new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .eq(Message::getIsRead, 0));
    }

    @Override
    public Message sendMessage(Long conversationId, Integer senderType, String senderId, String senderName,
                                String content, Integer messageType) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setIsRead(0);
        this.save(message);
        return message;
    }

    @Override
    public void markAsRead(Long conversationId, String receiverId) {
        // 将该会话中非 receiver 发送的消息标记为已读
        this.update(null, new LambdaQueryWrapper<Message>()
                .eq(Message::getConversationId, conversationId)
                .ne(Message::getSenderId, receiverId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
    }
}
