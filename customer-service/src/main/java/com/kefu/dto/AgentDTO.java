package com.kefu.dto;

import lombok.Data;

@Data
public class AgentDTO {
    private Long id;
    private String username;
    private String nickname;
    private Integer role;
    private Integer status;
}
