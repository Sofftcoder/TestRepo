package com.osc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Products {
    private String productId;

    private String categoryId;

    private String prodName;

    private Float prodMarketPrice;

    private String productDescription;

    private Integer ViewCount;
}
