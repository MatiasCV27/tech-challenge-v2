package com.challange.inventory_service.service;

import com.challange.inventory_service.Enum.StockOperation;
import com.challange.inventory_service.dto.ProductDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface IProductService {

    Mono<ProductDTO> getProduct(String sku);
    Flux<ProductDTO> getAllProducts();
    Flux<ProductDTO> searchProduct(String sku, String name, String description, String category, Boolean enabled);
    Mono<ProductDTO> createProduct(ProductDTO product);
    Flux<ProductDTO> addBulkProduct(List<ProductDTO> products);
    Mono<ProductDTO> updateProduct(String sku, ProductDTO product);
    Mono<ProductDTO> updateStockProduct(String sku, Integer quantity, StockOperation operation);
    Mono<ProductDTO> disabledProduct(String sku);
}