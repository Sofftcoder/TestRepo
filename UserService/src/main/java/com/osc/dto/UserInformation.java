package com.osc.dto;

import lombok.*;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInformation {

    @NotEmpty
    private String name;
    @NotEmpty
    @Email
    private String email;
    @NotEmpty
    @Size(min = 10,max = 10)
    private String contact;
    @NotEmpty
    private String DOB;
    private String user_Id;
}
