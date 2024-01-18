package com.osc.entity;

import com.hazelcast.nio.serialization.Serializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Products implements Serializable {
    @Id
    @Column(name = "ProductId")
    private String productId;
    @Column(name="CategoryId")
    private String categoryId;

    @Column(name = "ProductName")
    private String ProdName;
    @Column(name = "ProductPrice")
    private Float prodMarketPrice;

    private String ProductDescription;

    private Integer ViewCount;
}
