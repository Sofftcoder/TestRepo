package com.osc.entity;

import jdk.jfr.Enabled;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

@Enabled
@NoArgsConstructor
@Entity
@AllArgsConstructor
@Data
public class User {

    @NotEmpty
    private String name;
    @Id
    @NotEmpty
    @Email
    private String email;
    @NotEmpty
    @Size(min = 10,max = 10)
    private String contact;
    @NotEmpty
    private String DOB;
    private String user_Id;

    private String password;

}
