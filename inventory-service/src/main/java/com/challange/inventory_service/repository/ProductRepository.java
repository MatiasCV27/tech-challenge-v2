package com.challange.inventory_service.repository;

import com.challange.inventory_service.entity.ProductEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface ProductRepository extends R2dbcRepository<ProductEntity, Long> {

    Mono<Boolean> existsBySku(String sku);

    Flux<ProductEntity> findAllBySkuIn(Collection<String> skus);

    Mono<ProductEntity> findBySku(String sku);

    @Query(value = "SELECT * FROM t_product WHERE " +
            "(:sku IS NULL OR :sku = '' OR sku ILIKE CONCAT('%', :sku, '%')) AND " +
            "(:name IS NULL OR :name = '' OR name ILIKE CONCAT('%', :name, '%')) AND " +
            "(:description IS NULL OR :description = '' OR description ILIKE CONCAT('%', :description, '%')) AND " +
            "(:category IS NULL OR :category = '' OR category ILIKE CONCAT('%', :category, '%')) AND " +
            "(:enabled IS NULL OR enabled = :enabled)")
    Flux<ProductEntity> searchProducts(
            String sku, String name, String description,
            String category, Boolean enabled);
}