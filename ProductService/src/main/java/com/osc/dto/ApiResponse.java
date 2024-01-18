package com.osc.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApiResponse {
    private int code;
    private ListOfNewUserDashBoardData dataObject;
}
