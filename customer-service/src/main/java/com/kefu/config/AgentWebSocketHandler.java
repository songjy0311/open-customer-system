package com.kefu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kefu.entity.Message;
import com.kefu.enums.ConversationStatus;
import com.kefu.enums.SenderType;
import com.kefu.service.ConversationService;
import com.kefu.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    @Lazy
    private VisitorWebSocketHandler visitorWebSocketHandler;

    private final Map<String, WebSocketSession> agentSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String agentId = getAgentId(session);
        String key = agentId != null ? agentId : session.getId();
        agentSessions.put(key, session);
        System.out.println("客服连接建立: " + key);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        String type = (String) data.get("type");

        switch (type) {
            case "AGENT_LOGIN":
                handleAgentLogin(session, data);
                break;
            case "AGENT_MESSAGE":
                handleAgentMessage(session, data);
                break;
            case "TAKE_CONVERSATION":
                handleTakeConversation(session, data);
                break;
            case "END_CONVERSATION":
                handleEndConversation(session, data);
                break;
            case "MARK_READ":
                handleMarkRead(session, data);
                break;
            default:
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String agentId = getAgentId(session);
        if (agentId != null) {
            agentSessions.remove(agentId);
        }
        System.out.println("客服连接关闭: " + agentId);
    }

    private void handleAgentLogin(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long agentId = ((Number) data.get("agentId")).longValue();
        String agentNickname = (String) data.get("nickname");

        // 获取排队列表并发送
        var queueList = conversationService.getQueueList();
        Map<String, Object> result = Map.of(
                "type", "QUEUE_UPDATE",
                "data", queueList
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(result)));
    }

    private void handleAgentMessage(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long conversationId = ((Number) data.get("conversationId")).longValue();
        String content = (String) data.get("content");
        Integer messageType = (Integer) data.getOrDefault("messageType", 1);
        String agentId = getAgentId(session);
        String agentNickname = (String) data.get("agentNickname");

        // 安全校验：确认该客服确实负责这个会话
        var conversation = conversationService.getById(conversationId);
        if (conversation == null) {
            System.err.println("会话不存在, conversationId=" + conversationId);
            return;
        }
        if (conversation.getAgentId() == null || !conversation.getAgentId().toString().equals(agentId)) {
            System.err.println("客服 " + agentId + " 试图向不属于他的会话 " + conversationId + " 发消息");
            return;
        }

        Message savedMessage = messageService.sendMessage(conversationId, SenderType.AGENT, agentId,
                agentNickname, content, messageType);

        // 1. 回显给客服自己
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "NEW_MESSAGE",
                "data", savedMessage
        ))));

        // 2. 推送给对应访客（用数据库中的 visitorId）
        visitorWebSocketHandler.sendToVisitor(conversation.getVisitorId(), objectMapper.writeValueAsString(Map.of(
                "type", "NEW_MESSAGE",
                "data", savedMessage
        )));

        // 3. 标记已读并通知访客
        List<Long> readIds = messageService.markAsRead(conversationId, agentId);
        if (!readIds.isEmpty()) {
            visitorWebSocketHandler.notifyVisitorMessagesRead(
                    conversation.getVisitorId(), conversationId, readIds);
        }
    }

    private void handleTakeConversation(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long conversationId = ((Number) data.get("conversationId")).longValue();
        Long agentId = ((Number) data.get("agentId")).longValue();
        String agentNickname = (String) data.get("agentNickname");

        conversationService.takeConversation(conversationId, agentId, agentNickname);

        var conversation = conversationService.getById(conversationId);
        if (conversation != null) {
            // 1. 通知访客已被接手，并附带历史消息
            visitorWebSocketHandler.notifyVisitorTaken(conversation.getVisitorId(), conversationId, agentNickname, agentId);

            // 2. 标记访客之前发送的消息为已读
            List<Long> readIds = messageService.markAsRead(conversationId, agentId.toString());
            if (!readIds.isEmpty()) {
                visitorWebSocketHandler.notifyVisitorMessagesRead(
                        conversation.getVisitorId(), conversationId, readIds);
            }
        }

        broadcastQueueUpdate();
    }

    private void handleEndConversation(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long conversationId = ((Number) data.get("conversationId")).longValue();

        conversationService.endConversation(conversationId);

        var conversation = conversationService.getById(conversationId);
        if (conversation != null) {
            visitorWebSocketHandler.notifyVisitorEnded(conversation.getVisitorId(), conversationId);
        }
    }

    private void handleMarkRead(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long conversationId = ((Number) data.get("conversationId")).longValue();
        String agentId = getAgentId(session);

        var conversation = conversationService.getById(conversationId);
        if (conversation != null) {
            List<Long> readIds = messageService.markAsRead(conversationId, agentId);
            if (!readIds.isEmpty()) {
                visitorWebSocketHandler.notifyVisitorMessagesRead(
                        conversation.getVisitorId(), conversationId, readIds);
            }
        }
    }

    public void broadcastToAgent(Long conversationId, Message message) throws IOException {
        var conversation = conversationService.getById(conversationId);
        if (conversation != null && conversation.getAgentId() != null) {
            String agentId = conversation.getAgentId().toString();
            WebSocketSession session = agentSessions.get(agentId);
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                        "type", "NEW_MESSAGE",
                        "data", message
                ))));
            }
        }
    }

    public void broadcastQueueUpdate() throws IOException {
        var queueList = conversationService.getQueueList();
        String message = objectMapper.writeValueAsString(Map.of(
                "type", "QUEUE_UPDATE",
                "data", queueList
        ));

        for (WebSocketSession s : agentSessions.values()) {
            if (s.isOpen()) {
                s.sendMessage(new TextMessage(message));
            }
        }
    }

    private String getAgentId(WebSocketSession session) {
        return (String) session.getAttributes().get("agentId");
    }
}