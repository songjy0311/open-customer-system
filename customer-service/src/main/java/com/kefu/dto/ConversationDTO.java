package com.kefu.dto;

import lombok.Data;

@Data
public class ConversationDTO {
    private Long id;
    private String visitorId;
    private String visitorNickname;
    private Long agentId;
    private String agentNickname;
    private Integer status;
    private Integer queuePosition;
    private String startedAt;
    private String endedAt;
    private String createdAt;
}
