package com.osc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListOfExistingDashboardData {

    private List<ExistingUserDashboardData> data;

}
