package com.challange.inventory_service.controller;

import com.challange.inventory_service.Enum.StockOperation;
import com.challange.inventory_service.dto.ProductDTO;
import com.challange.inventory_service.service.IProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Tag(name = "Product", description = "Endpoints for managing inventory products")
public class ProductController {

    private final IProductService service;

    @GetMapping("/{sku}")
    @Operation(
            summary = "Get product by SKU",
            description = "Retrieves details for a specific product using its unique SKU identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product found successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public Mono<ProductDTO> getProduct(@PathVariable String sku) {
        return service.getProduct(sku);
    }

    @GetMapping()
    @Operation(
            summary = "Get all products",
            description = "Retrieves a comprehensive list of all registered products in the inventory."
    )
    @ApiResponse(responseCode = "200", description = "List of products retrieved successfully")
    public Flux<ProductDTO> getAllProducts() {
        return service.getAllProducts();
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search products dynamically",
            description = "Filters products based on optional parameters: SKU, name, description, category, or enabled status."
    )
    @ApiResponse(responseCode = "200", description = "Filtered product list retrieved successfully")
    public Flux<ProductDTO> searchProducts(
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean enabled) {
        return service.searchProduct(sku, name, description, category, enabled);
    }

    @PostMapping
    @Operation(
            summary = "Create a new product",
            description = "Registers a new product in the system with a unique SKU and initial stock."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or duplicated SKU")
    })
    public Mono<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        return service.createProduct(productDTO);
    }

    @PostMapping("/bulk")
    @Operation(
            summary = "Bulk create products",
            description = "Registers a list of multiple products in the system within a single transaction."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Products created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or duplicate SKU found in list or database")
    })
    public Flux<ProductDTO> addBulkProduct(@RequestBody List<ProductDTO> productDTOs) {
        return service.addBulkProduct(productDTOs);
    }

    @PatchMapping("/{sku}")
    @Operation(
            summary = "Partially update product details",
            description = "Updates descriptive fields (name, description, category, price, enabled status) of an existing product."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public Mono<ProductDTO> addProduct(@PathVariable String sku,
                                       @RequestBody ProductDTO productDTO) {
        return service.updateProduct(sku, productDTO);
    }

    @PatchMapping("/{sku}/stock")
    @Operation(
            summary = "Adjust product stock level",
            description = "Increases (ADD) or decreases (SUBTRACT) product inventory stock level."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stock updated successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient stock or invalid quantity"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public Mono<ProductDTO> updateStockProduct(
            @PathVariable String sku,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) StockOperation operation) {
        return service.updateStockProduct(sku, quantity, operation);
    }

    @PatchMapping("/{sku}/disable")
    @Operation(
            summary = "Disable a product (Soft Delete)",
            description = "Marks a product as disabled, preventing it from being sold or assigned to new orders."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product disabled successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public Mono<ProductDTO> disabledProduct(@PathVariable String sku) {
        return service.disabledProduct(sku);
    }
}