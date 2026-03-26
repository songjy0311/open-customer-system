package com.kefu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kefu.entity.Conversation;
import com.kefu.enums.ConversationStatus;
import com.kefu.mapper.ConversationMapper;
import com.kefu.service.ConversationService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationServiceImpl extends ServiceImpl<ConversationMapper, Conversation>
        implements ConversationService {

    @Override
    public List<Conversation> getQueueList() {
        return this.list(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getStatus, ConversationStatus.WAITING)
                .orderByAsc(Conversation::getQueuePosition)
                .orderByAsc(Conversation::getCreatedAt));
    }

    @Override
    public List<Conversation> getActiveConversations(Long agentId) {
        return this.list(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getAgentId, agentId)
                .in(Conversation::getStatus, ConversationStatus.WAITING, ConversationStatus.IN_PROGRESS)
                .orderByDesc(Conversation::getCreatedAt));
    }

    @Override
    public boolean takeConversation(Long conversationId, Long agentId, String agentNickname) {
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setAgentId(agentId);
        conversation.setAgentNickname(agentNickname);
        conversation.setStatus(ConversationStatus.IN_PROGRESS);
        conversation.setStartedAt(LocalDateTime.now());
        return this.updateById(conversation);
    }

    @Override
    public boolean transferConversation(Long conversationId, Long newAgentId) {
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setAgentId(newAgentId);
        return this.updateById(conversation);
    }

    @Override
    public boolean endConversation(Long conversationId) {
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setStatus(ConversationStatus.ENDED);
        conversation.setEndedAt(LocalDateTime.now());
        return this.updateById(conversation);
    }

    @Override
    public Conversation createConversation(String visitorId, String visitorNickname, String visitorIp) {
        // 获取当前排队位置
        long queueCount = this.count(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getStatus, ConversationStatus.WAITING));

        Conversation conversation = new Conversation();
        conversation.setVisitorId(visitorId);
        conversation.setVisitorNickname(visitorNickname);
        conversation.setVisitorIp(visitorIp);
        conversation.setStatus(ConversationStatus.WAITING);
        conversation.setQueuePosition((int) queueCount + 1);
        this.save(conversation);
        return conversation;
    }
}
