package com.kefu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kefu.entity.Conversation;

import java.util.List;

public interface ConversationService extends IService<Conversation> {
    /**
     * 获取排队列表
     */
    List<Conversation> getQueueList();

    /**
     * 获取当前客服的进行中会话
     */
    List<Conversation> getActiveConversations(Long agentId);

    /**
     * 接手会话
     */
    boolean takeConversation(Long conversationId, Long agentId, String agentNickname);

    /**
     * 转接会话
     */
    boolean transferConversation(Long conversationId, Long newAgentId);

    /**
     * 结束会话
     */
    boolean endConversation(Long conversationId);

    /**
     * 创建新会话（用户发起咨询）
     */
    Conversation createConversation(String visitorId, String visitorNickname, String visitorIp);
}
