package com.osc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Otp {

    @NotEmpty
    @Email
    private String userId;
    @NotEmpty
    @JsonProperty("OTP")
    private String OTP;

}
