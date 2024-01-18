package com.osc.dto;

import lombok.Data;

@Data
public class DataObject {
    private String response;
    private String sessionId;
    private String name;
    public DataObject(String response) {
        this.response=response;
    }

    public DataObject(String sessionId,String name){
        this.sessionId = sessionId;
        this.name = name;
    }
}
