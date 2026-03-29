package com.kefu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kefu.entity.Conversation;
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
public class VisitorWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    @Lazy
    private AgentWebSocketHandler agentWebSocketHandler;

    private final Map<String, WebSocketSession> visitorSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String visitorId = getVisitorId(session);
        String key = visitorId != null ? visitorId : session.getId();
        visitorSessions.put(key, session);
        System.out.println("访客连接建立: " + key);
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
        if (visitorId != null) {
            visitorSessions.remove(visitorId);
        }
        System.out.println("访客连接关闭: " + visitorId);
    }

    private void handleVisitorJoin(WebSocketSession session, Map<String, Object> data) throws IOException {
        String visitorId = (String) data.get("visitorId");
        String nickname = (String) data.getOrDefault("nickname", "访客");
        String visitorIp = getClientIp(session);

        Conversation conversation = conversationService.createConversation(visitorId, nickname, visitorIp);

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "CONVERSATION_STARTED",
                "data", Map.of(
                        "conversationId", conversation.getId(),
                        "status", conversation.getStatus(),
                        "queuePosition", conversation.getQueuePosition()
                )
        ))));
    }

    private void handleVisitorMessage(WebSocketSession session, Map<String, Object> data) throws IOException {
        Long conversationId = ((Number) data.get("conversationId")).longValue();
        String content = (String) data.get("content");
        Integer messageType = (Integer) data.getOrDefault("messageType", 1);
        String visitorId = getVisitorId(session);

        Message savedMessage = messageService.sendMessage(conversationId, SenderType.VISITOR, visitorId,
                "访客", content, messageType);

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "NEW_MESSAGE",
                "data", savedMessage
        ))));

        agentWebSocketHandler.broadcastToAgent(conversationId, savedMessage);
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

    public void notifyVisitorTaken(String visitorId, Long conversationId, String agentNickname, Long agentId) throws IOException {
        // 获取历史消息
        List<Message> historyMessages = messageService.getHistoryMessages(conversationId);

        Map<String, Object> result = Map.of(
                "type", "CONVERSATION_STARTED",
                "data", Map.of(
                        "conversationId", conversationId,
                        "agentNickname", agentNickname,
                        "status", ConversationStatus.IN_PROGRESS,
                        "historyMessages", historyMessages
                )
        );
        sendToVisitor(visitorId, objectMapper.writeValueAsString(result));
    }

    public void notifyVisitorMessagesRead(String visitorId, Long conversationId, List<Long> readMessageIds) throws IOException {
        sendToVisitor(visitorId, objectMapper.writeValueAsString(Map.of(
                "type", "MESSAGES_READ",
                "data", Map.of(
                        "conversationId", conversationId,
                        "readMessageIds", readMessageIds
                )
        )));
    }

    public void notifyVisitorEnded(String visitorId, Long conversationId) throws IOException {
        sendToVisitor(visitorId, objectMapper.writeValueAsString(Map.of(
                "type", "CONVERSATION_ENDED",
                "data", Map.of("conversationId", conversationId)
        )));
    }

    private String getVisitorId(WebSocketSession session) {
        return (String) session.getAttributes().get("visitorId");
    }

    private String getClientIp(WebSocketSession session) {
        return session.getRemoteAddress().getAddress().getHostAddress();
    }
}