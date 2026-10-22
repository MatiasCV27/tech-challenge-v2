package com.challange.inventory_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "t_product")
public class ProductEntity {

    @Id
    private Long id;
    private String sku;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer stock;
    private Boolean enabled;
}
