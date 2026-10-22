package com.challange.inventory_service.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProductDTO(
    String sku,
    String name,
    String description,
    String category,
    BigDecimal price,
    Integer stock,
    Boolean enabled) {
}