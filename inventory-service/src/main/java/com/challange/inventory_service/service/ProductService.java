package com.challange.inventory_service.service;

import com.challange.inventory_service.Enum.StockOperation;
import com.challange.inventory_service.dto.ProductDTO;
import com.challange.inventory_service.mapper.ProductMapper;
import com.challange.inventory_service.entity.ProductEntity;
import com.challange.inventory_service.exception.NotFoundException;
import com.challange.inventory_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Override
    public Mono<ProductDTO> getProduct(String sku) {

        log.info("Searching product with SKU: {}", sku);

        return repository.findBySku(sku)
                .switchIfEmpty(Mono.error(new NotFoundException("Product not found")))
                .map(productEntity -> {
                    log.warn("Product found with SKU: {}", sku);
                    return mapper.mapToProductDTO(productEntity);
                });
    }

    @Override
    public Flux<ProductDTO> getAllProducts() {
        log.info("Querying all registered products");

        return repository.findAll().map(mapper::mapToProductDTO);
    }

    @Override
    public Flux<ProductDTO> searchProduct(String sku, String name, String description, String category, Boolean enabled) {

        log.info("Searching products with filters - SKU: {}, name: {}, category: {}, enabled: {}", sku, name, category, enabled);

        return repository.searchProducts(sku, name, description, category, enabled).map(mapper::mapToProductDTO);
    }

    @Override
    public Mono<ProductDTO> createProduct(ProductDTO product) {

        log.info("Initiating creation process for product with SKU: {}", product.sku());

        return repository.existsBySku(product.sku())
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Attempt to create a duplicate product with SKU: {}", product.sku());
                        return Mono.error(new IllegalArgumentException("The product with the SKU " + product.sku() + " already exists"));
                    }

                    ProductEntity prod = mapper.mapToProductEntity(product);
                    return repository.save(prod);
                })
                .map(savedProduct -> {
                    log.info("Product successfully created with SKU: {}", savedProduct.getSku());
                    return mapper.mapToProductDTO(savedProduct);
                });
    }

    @Override
    public Flux<ProductDTO> addBulkProduct(List<ProductDTO> products) {
        log.info("Initiating bulk product creation process for {} items", products.size());

        Set<String> inputSkus = new HashSet<>();
        for (ProductDTO product : products) {
            if (!inputSkus.add(product.sku())) {
                log.warn("Duplicate SKU found in payload during bulk insert: {}", product.sku());
                return Flux.error(new IllegalArgumentException("Duplicate SKU found in payload: " + product.sku()));
            }
        }

        List<String> skusList = products.stream().map(ProductDTO::sku).toList();

        return repository.findAllBySkuIn(skusList)
                .collectList()
                .flatMapMany(existingEntities -> {
                    if (!existingEntities.isEmpty()) {
                        List<String> existingSkus = existingEntities.stream().map(ProductEntity::getSku).toList();
                        log.warn("Bulk insert aborted. The following SKUs already exist in database: {}", existingSkus);
                        return Flux.error(new IllegalArgumentException("The following SKUs already exist in the database: " + existingSkus));
                    }

                    List<ProductEntity> entities = products.stream().map(mapper::mapToProductEntity).toList();

                    return repository.saveAll(entities);
                })
                .map(savedEntity -> {
                    log.info("Product successfully saved in bulk with SKU: {}", savedEntity.getSku());
                    return mapper.mapToProductDTO(savedEntity);
                });
    }

    @Override
    public Mono<ProductDTO> updateProduct(String sku, ProductDTO product) {
        log.info("Request to update product with SKU: {}", sku);

        return repository.findBySku(sku)
                .switchIfEmpty(Mono.error(new NotFoundException("Product not found with SKU: " + sku)))
                .flatMap(prod -> {
                    if (StringUtils.hasText(product.name())) prod.setName(product.name());
                    if (StringUtils.hasText(product.description())) prod.setDescription(product.description());
                    if (StringUtils.hasText(product.category())) prod.setCategory(product.category());
                    if (product.price() != null) prod.setPrice(product.price());
                    if (product.enabled() != null) prod.setEnabled(product.enabled());

                    log.info("Saving updated product with SKU: {}", sku);
                    return repository.save(prod);
                })
                .map(updatedProduct -> {
                    log.info("Product with SKU: {} successfully updated", sku);
                    return mapper.mapToProductDTO(updatedProduct);
                });
    }

    @Override
    public Mono<ProductDTO> updateStockProduct(String sku, Integer quantity, StockOperation operation) {
        log.info("Stock operation request: {} of {} units for product SKU: {}", operation, quantity, sku);

        if (quantity == null || quantity <= 0) {
            log.warn("Invalid stock quantity provided: {}", quantity);
            return Mono.error(new IllegalArgumentException("Quantity must be greater than zero"));
        }

        return repository.findBySku(sku)
                .switchIfEmpty(Mono.error(new NotFoundException("Product not found for stock operation with SKU: " + sku)))
                .flatMap(prod -> {
                    if (StockOperation.SUBTRACT.equals(operation)) {
                        if (!Boolean.TRUE.equals(prod.getEnabled())) {
                            log.error("Attempted to reduce stock for disabled product SKU: {}", sku);
                            return Mono.error(new IllegalStateException("Cannot reduce stock for a disabled product"));
                        }

                        if (prod.getStock() < quantity) {
                            log.warn("Insufficient stock for SKU: {}. Requested: {}, Available: {}", sku, quantity, prod.getStock());
                            return Mono.error(new IllegalArgumentException("Insufficient stock for SKU: " + sku + ". Available: " + prod.getStock()));
                        }

                        prod.setStock(prod.getStock() - quantity);
                        log.debug("Subtracted {} units from SKU: {}. New stock: {}", quantity, sku, prod.getStock());

                    } else if (StockOperation.ADD.equals(operation)) {
                        prod.setStock(prod.getStock() + quantity);
                        log.debug("Added {} units to SKU: {}. New stock: {}", quantity, sku, prod.getStock());
                    }

                    return repository.save(prod);
                })
                .map(savedProduct -> {
                    log.info("Product with SKU: {} successfully updated after stock operation", sku);
                    return mapper.mapToProductDTO(savedProduct);
                });
    }

    @Override
    public Mono<ProductDTO> disabledProduct(String sku) {
        log.info("Request to disable product with SKU: {}", sku);

        return repository.findBySku(sku)
                .switchIfEmpty(Mono.error(new NotFoundException("Product not found for disabling with SKU: " + sku)))
                .flatMap(productEntity -> {
                    productEntity.setEnabled(false);
                    log.info("Saving disabled state for product with SKU: {}", sku);

                    return repository.save(productEntity);
                })
                .map(savedProduct -> {
                    log.info("Product with SKU: {} successfully disabled", sku);
                    return mapper.mapToProductDTO(savedProduct);
                });
    }
}
