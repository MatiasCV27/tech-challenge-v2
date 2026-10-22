package com.challange.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", url = "${inventory.service.url}")
public interface InventoryClient {

    @PatchMapping("/api/products/{sku}/stock")
    void updateStockProduct(@PathVariable("sku") String sku,
                            @RequestParam("quantity") Integer quantity, @RequestParam("operation") String operation
    );
}
