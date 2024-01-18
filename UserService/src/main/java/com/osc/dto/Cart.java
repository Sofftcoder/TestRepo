package com.osc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    private String userId;
    private String ProductsCartCount;
    private List<Cart1> cartProducts;
    private String totalPrice;
}
