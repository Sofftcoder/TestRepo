package com.osc.entity;

import com.hazelcast.shaded.com.fasterxml.jackson.jr.ob.JSON;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.protocol.types.Field;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserData {
    @Id
    private String userId;

    private String cartDetails;

    private String recentlyViewedDetails;
}
