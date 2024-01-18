package com.osc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponsse {
    private int code;
    private ListOfExistingDashboardData dataObject;
}
