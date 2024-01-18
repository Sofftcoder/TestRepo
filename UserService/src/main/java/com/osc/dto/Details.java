package com.osc.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Data
@NoArgsConstructor
public class Details {
    @NotEmpty
    private String userId;
    @NotEmpty
    private String sessionId;
    @NotEmpty
    private String loginDevice;
    public Details(String userId, String sessionId, String loginDevice) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.loginDevice = loginDevice;
    }
}
