package com.osc.entity;

import lombok.*;
import org.apache.kafka.common.protocol.types.Field;

import javax.annotation.sql.DataSourceDefinitions;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.validation.constraints.NotEmpty;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Session {
    @Id
    private String userId;
    @NotEmpty
    private String deviceId;

    private String sessionId;
    private String loginTime;
    private String logoutTime;

}
