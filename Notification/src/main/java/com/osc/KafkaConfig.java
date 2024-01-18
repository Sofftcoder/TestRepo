package com.osc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hazelcast.internal.json.JsonObject;
import com.osc.service.NotificationService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.naming.IdentityNamingStrategy;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.Map;

@Configuration
public class KafkaConfig {

    @Autowired
    NotificationService notificationService;


    @KafkaListener(topics = AppConstants.TOPIC, groupId = AppConstants.GROUP_ID)
    public void notification(Map<String,String> data){
            String name = data.get("name");
            String email = data.get("email");
            notificationService.sendMail(email);
            notificationService.welcomeMessage(name, email);
    }
}