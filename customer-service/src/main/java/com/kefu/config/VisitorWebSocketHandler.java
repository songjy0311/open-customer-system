package com.kefu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;

@Component
public class VisitorWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private MessageService messageService;

    @Autowired
    @Lazy
    private AgentWebSocketHandler agentWebSocketHandler;

    /** 当前活跃的访客 WebSocket 会话，按 visitorId 索引 */
    private final Map<String, WebSocketSession> visitorSessions = new ConcurrentHashMap<>();
    /** 暂存消息队列，访客重连后按顺序发送 */
    private final Map<String, Queue<String>> pendingMessages = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String visitorId = getVisitorId(session);
        System.out.println(">>> afterConnectionEstablished: visitorId=" + visitorId + ", sessionId=" + session.getId() + ", params=" + session.getAttributes());
        String key = visitorId != null ? visitorId : session.getId();
        visitorSessions.put(key, session);
        System.out.println("访客连接建立: " + key + ", 当前在线: " + visitorSessions.keySet());

        // 重连时发送所有暂存消息（按顺序）
        Queue<String> queue = pendingMessages.remove(key);
        if (queue != null && !queue.isEmpty()) {
            System.out.println("发送暂存消息给 " + key + "，共 " + queue.size() + " 条");
            for (String msg : queue) {
                session.sendMessage(new TextMessage(msg));
            }
        }
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
        System.out.println(">>> afterConnectionClosed: visitorId=" + visitorId + ", status=" + status);
        if (visitorId != null) {
            visitorSessions.remove(visitorId);
            System.out.println("访客连接关闭: " + visitorId + ", status=" + status + ", 剩余在线: " + visitorSessions.keySet());
        }
    }

    private void handleVisitorJoin(WebSocketSession session, Map<String, Object> data) throws IOException {
        String visitorId = (String) data.get("visitorId");
        System.out.println(">>> handleVisitorJoin: visitorId=" + visitorId + ", sessionId=" + session.getId());
        System.out.println(">>> 已有会话列表: " + visitorSessions.keySet());

        String nickname = (String) data.getOrDefault("nickname", "访客");
        String visitorIp = getClientIp(session);

        // 查找访客的任何未结束会话（包括 WAITING 和 IN_PROGRESS），复用而非新建
        var existing = conversationService.list(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getVisitorId, visitorId)
                .ne(Conversation::getStatus, ConversationStatus.ENDED)
                .orderByDesc(Conversation::getId)
                .last("LIMIT 1"));
        if (!existing.isEmpty()) {
            Conversation conv = existing.get(0);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "CONVERSATION_STARTED",
                    "data", Map.of(
                            "conversationId", conv.getId(),
                            "status", conv.getStatus(),
                            "queuePosition", conv.getQueuePosition(),
                            "agentNickname", conv.getAgentNickname() != null ? conv.getAgentNickname() : ""
                    )
            ))));
            return;
        }

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

    /**
     * 向访客发送消息。如果连接断开，消息进入暂存队列，访客重连时自动发送。
     */
    public void sendToVisitor(String visitorId, String message) throws IOException {
        System.out.println(">>> sendToVisitor: visitorId=" + visitorId + ", messageLen=" + message.length());
        WebSocketSession session = visitorSessions.get(visitorId);
        System.out.println(">>> session found: " + (session != null) + ", isOpen: " + (session != null && session.isOpen()));
        if (session != null && session.isOpen()) {
            session.sendMessage(new TextMessage(message));
        } else {
            System.out.println(">>> 访客未在线，消息进入暂存队列");
            pendingMessages.computeIfAbsent(visitorId, k -> new LinkedList<>()).add(message);
        }
    }

    public void notifyVisitorTaken(String visitorId, Long conversationId, String agentNickname, Long agentId) {
        try {
            System.out.println(">>> notifyVisitorTaken: visitorId=" + visitorId + ", convId=" + conversationId + ", agent=" + agentNickname);
            System.out.println(">>> 当前在线访客会话: " + visitorSessions.keySet());
            System.out.println(">>> 暂存消息队列: " + pendingMessages.keySet());

            List<Message> historyMessages = messageService.getHistoryMessages(conversationId);

            var conv = conversationService.getById(conversationId);
            Integer queuePosition = conv != null ? conv.getQueuePosition() : 0;
            Integer currentStatus = conv != null ? conv.getStatus() : 0;

            long aheadCount = 0;
            if (queuePosition != null && queuePosition > 1) {
                aheadCount = conversationService.count(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getStatus, ConversationStatus.WAITING)
                        .lt(Conversation::getQueuePosition, queuePosition));
            }

            Map<String, Object> msgData = new HashMap<>();
            msgData.put("conversationId", conversationId);
            msgData.put("agentNickname", agentNickname);
            msgData.put("status", currentStatus);
            msgData.put("historyMessages", historyMessages != null ? historyMessages : List.of());
            msgData.put("aheadCount", aheadCount);

            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "CONVERSATION_STARTED",
                    "data", msgData
            ));

            System.out.println(">>> 发送 CONVERSATION_STARTED: " + json);

            sendToVisitor(visitorId, json);
        } catch (Exception e) {
            System.err.println("notifyVisitorTaken: 异常 " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void notifyVisitorMessagesRead(String visitorId, Long conversationId, List<Long> readMessageIds) {
        try {
            sendToVisitor(visitorId, objectMapper.writeValueAsString(Map.of(
                    "type", "MESSAGES_READ",
                    "data", Map.of(
                            "conversationId", conversationId,
                            "readMessageIds", readMessageIds
                    )
            )));
        } catch (Exception e) {
            System.err.println("notifyVisitorMessagesRead: 异常 " + e.getMessage());
        }
    }

    public void notifyVisitorEnded(String visitorId, Long conversationId) {
        try {
            sendToVisitor(visitorId, objectMapper.writeValueAsString(Map.of(
                    "type", "CONVERSATION_ENDED",
                    "data", Map.of("conversationId", conversationId)
            )));
        } catch (Exception e) {
            System.err.println("notifyVisitorEnded: 异常 " + e.getMessage());
        }
    }

    private String getVisitorId(WebSocketSession session) {
        return (String) session.getAttributes().get("visitorId");
    }

    private String getClientIp(WebSocketSession session) {
        return session.getRemoteAddress().getAddress().getHostAddress();
    }
}