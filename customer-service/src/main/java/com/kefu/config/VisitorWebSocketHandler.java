package com.kefu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kefu.dto.Result;
import com.kefu.entity.Conversation;
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
public class VisitorWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private AgentWebSocketHandler agentWebSocketHandler;

    private final Map<String, WebSocketSession> visitorSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String visitorId = getVisitorId(session);
        visitorSessions.put(visitorId, session);
        System.out.println("访客连接建立: " + visitorId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> data = objectMapper.readValue(payload, Map.class);
        String type = (String) data.get("type");

        switch (type) {
            case "VISITOR_JOIN":
                handleVisitorJoin(session, data);
                break;
            case "VISITOR_MESSAGE":
                handleVisitorMessage(session, data);
                break;
            case "MESSAGE_READ":
                handleMessageRead(session, data);
                break;
            default:
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String visitorId = getVisitorId(session);
        visitorSessions.remove(visitorId);
        System.out.println("访客连接关闭: " + visitorId);
    }

    private void handleVisitorJoin(WebSocketSession session, Map<String, Object> data) throws IOException {
        String visitorId = (String) data.get("visitorId");
        String nickname = (String) data.getOrDefault("nickname", "访客");
        String visitorIp = getClientIp(session);

        // 创建会话
        Conversation conversation = conversationService.createConversation(visitorId, nickname, visitorIp);

        // 返回会话信息
        Map<String, Object> result = Map.of(
                "type", "CONVERSATION_STARTED",
                "data", Map.of(
                        "conversationId", conversation.getId(),
                        "status", conversation.getStatus(),
                        "queuePosition", conversation.getQueuePosition()
                )
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(result)));
    }

    private void handleVisitorMessage(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long conversationId = ((Number) data.get("conversationId")).longValue();
        String content = (String) data.get("content");
        Integer messageType = (Integer) data.getOrDefault("messageType", 1);
        String visitorId = getVisitorId(session);

        // 保存消息
        Message message = messageService.sendMessage(conversationId, SenderType.VISITOR, visitorId,
                "访客", content, messageType);

        // 转发给客服
        agentWebSocketHandler.broadcastToAgent(conversationId, message);

        // 返回发送成功
        Map<String, Object> result = Map.of(
                "type", "MESSAGE_SENT",
                "data", Map.of("messageId", message.getId())
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(result)));
    }

    private void handleMessageRead(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long conversationId = ((Number) data.get("conversationId")).longValue();
        String visitorId = getVisitorId(session);

        messageService.markAsRead(conversationId, visitorId);
    }

    public void sendToVisitor(String visitorId, String message) throws IOException {
        WebSocketSession session = visitorSessions.get(visitorId);
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        }
    }

    public void notifyVisitorTaken(String visitorId, Long conversationId, String agentNickname) throws IOException {
        Map<String, Object> result = Map.of(
                "type", "CONVERSATION_STARTED",
                "data", Map.of(
                        "conversationId", conversationId,
                        "agentNickname", agentNickname,
                        "status", ConversationStatus.IN_PROGRESS
                )
        );
        sendToVisitor(visitorId, objectMapper.writeValueAsString(result));
    }

    public void notifyVisitorEnded(String visitorId, Long conversationId) throws IOException {
        Map<String, Object> result = Map.of(
                "type", "CONVERSATION_ENDED",
                "data", Map.of("conversationId", conversationId)
        );
        sendToVisitor(visitorId, objectMapper.writeValueAsString(result));
    }

    private String getVisitorId(WebSocketSession session) {
        return (String) session.getAttributes().get("visitorId");
    }

    private String getClientIp(WebSocketSession session) {
        return session.getRemoteAddress().getAddress().getHostAddress();
    }
}
