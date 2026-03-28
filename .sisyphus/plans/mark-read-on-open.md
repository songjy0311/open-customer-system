# 客服打开会话时自动标记已读

## TL;DR

> **Quick Summary**: 客服在"进行会话"页面点击某条会话打开聊天记录时，立即将该会话中访客发送的未读消息标记为已读，并通过 WebSocket 推送 MESSAGES_READ 事件给访客，使访客侧消息状态从"未读"变为"已读"。
>
> **Deliverables**:
> - 后端 `AgentWebSocketHandler.java` 新增 `MARK_READ` 消息处理
> - 前端 `AgentDashboard.tsx` 的 `loadMessages` 成功后发送 `MARK_READ` WebSocket 消息
>
> **Estimated Effort**: Quick
> **Parallel Execution**: NO - sequential (后端先，前端后，最后验证)
> **Critical Path**: Task 1 → Task 2 → Task 3

---

## Context

### 现有已读链路

`markAsRead` 目前只在两处调用：
1. 客服**接手**会话时（`handleTakeConversation` + `ConversationController.takeConversation`）
2. 客服**发送消息**时（`handleAgentMessage`）

缺少场景：客服打开某个已接手的会话浏览消息时，不会触发已读，访客消息永远显示"未读"直到客服回复。

### 修复方案

前端 `loadMessages()` 成功后，通过 WebSocket 发送 `MARK_READ` 消息（携带 `conversationId` 和 `agentId`）；后端新增对该消息类型的处理，调用已有的 `markAsRead` + `notifyVisitorMessagesRead`。

### 关键文件

- `customer-service/src/main/java/com/kefu/config/AgentWebSocketHandler.java`
- `customer-webapp/src/pages/agent/AgentDashboard.tsx`

---

## Work Objectives

### Core Objective
客服打开会话时自动标记已读并实时通知访客。

### Must Have
- 后端处理 `MARK_READ` WebSocket 消息
- 前端 `loadMessages` 成功后发送 `MARK_READ`
- `mvn compile` BUILD SUCCESS
- `tsc --noEmit` 零错误

### Must NOT Have
- 不新增任何 REST 接口
- 不引入新依赖
- 不添加不必要的注释
- 不使用 `as any` 或 `@ts-ignore`

---

## TODOs

- [ ] 1. 后端 `AgentWebSocketHandler.java` 新增 `MARK_READ` 处理

  **What to do**:

  在 `handleTextMessage` 的 switch 里，`END_CONVERSATION` case 之后新增：
  ```java
  case "MARK_READ":
      handleMarkRead(session, data);
      break;
  ```

  在 `handleEndConversation` 方法之后新增私有方法：
  ```java
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
  ```

  **References**:
  - `AgentWebSocketHandler.java:124-143` — `handleTakeConversation` 的 markAsRead 调用模式，完全复用
  - `AgentWebSocketHandler.java:52-67` — switch 结构，在 END_CONVERSATION case 后追加

  **Acceptance Criteria**:
  - [ ] `mvn compile` BUILD SUCCESS，无编译错误

---

- [ ] 2. 前端 `AgentDashboard.tsx` `loadMessages` 成功后发送 `MARK_READ`

  **What to do**:

  找到 `loadMessages` 函数（当前约 132-140 行），在 `setMessages(res.data || [])` 之后，紧接着通过 wsRef 发送 MARK_READ：

  ```typescript
  const loadMessages = async (conversationId: number) => {
    try {
      const res = await api.get(`/message/history/${conversationId}`)
      if (res.code === 200) {
        setMessages(res.data || [])
        wsRef.current?.send({
          type: 'MARK_READ',
          conversationId,
          agentId: Number(agentId)
        })
      }
    } catch (error) {
      console.error('Load messages error:', error)
    }
  }
  ```

  **References**:
  - `AgentDashboard.tsx:132-140` — 当前 `loadMessages` 函数全文
  - `AgentDashboard.tsx:143-153` — `handleSendMessage` 中 `wsRef.current?.send` 的调用方式

  **Acceptance Criteria**:
  - [ ] `tsc --noEmit` 零错误

---

- [ ] 3. 编译验证

  **What to do**:
  - 运行 `/Users/songjiayin/Leibaoxin/Config/apache-maven-3.8.4/bin/mvn compile -q` 在 `customer-service/` 目录
  - 运行 `npx tsc --noEmit` 在 `customer-webapp/` 目录
  - 两者均必须无错误

  **Acceptance Criteria**:
  - [ ] `mvn compile` 输出无 ERROR
  - [ ] `tsc --noEmit` 无任何输出（零错误）

---

## Success Criteria

- 客服点击"进行会话"列表中某条会话 → `loadMessages` 调用 → WebSocket 发送 `MARK_READ` → 后端 `handleMarkRead` 触发 → `markAsRead` 更新数据库 → `notifyVisitorMessagesRead` 推送给访客 → 访客消息状态变为"已读"
- 两端编译验证通过
