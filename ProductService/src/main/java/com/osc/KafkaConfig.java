package com.osc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.osc.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;

@Configuration
public class KafkaConfig {

    @Autowired
    ProductService productService;
    @KafkaListener(topics = "data",groupId = "group-1")
    public void notification(String prodId) throws JsonProcessingException {
        productService.dataUpdationOnLogOut();
    }
}