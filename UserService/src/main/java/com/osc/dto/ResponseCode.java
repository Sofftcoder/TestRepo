package com.osc.dto;

import com.osc.dto.DataObject;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResponseCode {
    private int code;
    private ResponseDataObject dataObject;

    public ResponseCode(int code){
        this.code = code;
    }
    public ResponseCode(int code,DataObject dataObject) {
        this.code = code;
        if (dataObject==null){
            this.dataObject=null;
        } else{
            this.dataObject=new ResponseDataObject(dataObject.getResponse());
        }

    }
}
