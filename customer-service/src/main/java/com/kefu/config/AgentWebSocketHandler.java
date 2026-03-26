package com.kefu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kefu.entity.Message;
import com.kefu.enums.ConversationStatus;
import com.kefu.enums.SenderType;
import com.kefu.service.ConversationService;
import com.kefu.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private VisitorWebSocketHandler visitorWebSocketHandler;

    private final Map<String, WebSocketSession> agentSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String agentId = getAgentId(session);
        agentSessions.put(agentId, session);
        System.out.println("客服连接建立: " + agentId);
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
            default:
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String agentId = getAgentId(session);
        agentSessions.remove(agentId);
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

        // 保存消息
        Message message = messageService.sendMessage(conversationId, SenderType.AGENT, agentId,
                agentNickname, content, messageType);

        // 转发给访客
        var conversation = conversationService.getById(conversationId);
        if (conversation != null) {
            visitorWebSocketHandler.sendToVisitor(conversation.getVisitorId(),
                    objectMapper.writeValueAsString(Map.of(
                            "type", "NEW_MESSAGE",
                            "data", message
                    )));
        }

        // 返回发送成功
        Map<String, Object> result = Map.of(
                "type", "MESSAGE_SENT",
                "data", Map.of("messageId", message.getId())
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(result)));
    }

    private void handleTakeConversation(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long conversationId = ((Number) data.get("conversationId")).longValue();
        Long agentId = ((Number) data.get("agentId")).longValue();
        String agentNickname = (String) data.get("agentNickname");

        // 接手会话
        conversationService.takeConversation(conversationId, agentId, agentNickname);

        // 通知访客
        var conversation = conversationService.getById(conversationId);
        if (conversation != null) {
            visitorWebSocketHandler.notifyVisitorTaken(conversation.getVisitorId(), conversationId, agentNickname);
        }

        // 通知所有客服更新排队列表
        broadcastQueueUpdate();
    }

    private void handleEndConversation(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long conversationId = ((Number) data.get("conversationId")).longValue();

        // 结束会话
        conversationService.endConversation(conversationId);

        // 通知访客
        var conversation = conversationService.getById(conversationId);
        if (conversation != null) {
            visitorWebSocketHandler.notifyVisitorEnded(conversation.getVisitorId(), conversationId);
        }
    }

    public void broadcastToAgent(Long conversationId, Message message) throws IOException {
        // 找到负责该会话的客服并发送消息
        var conversation = conversationService.getById(conversationId);
        if (conversation != null && conversation.getAgentId() != null) {
            String agentId = conversation.getAgentId().toString();
            WebSocketSession session = agentSessions.get(agentId);
            if (session != null && session.isOpen()) {
                Map<String, Object> result = Map.of(
                        "type", "NEW_MESSAGE",
                        "data", message
                );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(result)));
            }
        }
    }

    public void broadcastQueueUpdate() throws IOException {
        var queueList = conversationService.getQueueList();
        Map<String, Object> result = Map.of(
                "type", "QUEUE_UPDATE",
                "data", queueList
        );
        String message = objectMapper.writeValueAsString(result);

        for (WebSocketSession session : agentSessions.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    private String getAgentId(WebSocketSession session) {
        return (String) session.getAttributes().get("agentId");
    }
}
