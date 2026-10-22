package com.challange.order_service.controller;

import com.challange.order_service.Enum.OrderStatus;
import com.challange.order_service.dto.OrderDTO;
import com.challange.order_service.dto.OrderStatusHistoryDTO;
import com.challange.order_service.service.IOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "Endpoints for managing orders")
public class OrderController {

    private final IOrderService service;

    @GetMapping("/{orderNumber}")
    @Operation(
            summary = "Get order by order number",
            description = "Retrieves details for a specific order using its unique orderNumber identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderDTO> getOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(service.getOrder(orderNumber));
    }

    @GetMapping
    @Operation(
            summary = "Get all orders",
            description = "Retrieves a comprehensive list of all registered orders."
    )
    @ApiResponse(responseCode = "200", description = "List of orders retrieved successfully")
    public ResponseEntity<List<OrderDTO>> getAllOrders() {
        return ResponseEntity.ok(service.getAllOrders());
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search orders dynamically",
            description = "Filters orders based on optional parameters: orderNumber, clientId, status, fromDate, and toDate."
    )
    @ApiResponse(responseCode = "200", description = "Filtered order list retrieved successfully")
    public ResponseEntity<List<OrderDTO>> searchOrders( // Cambiado de searchProducts a searchOrders
                                                        @RequestParam(required = false) String orderNumber,
                                                        @RequestParam(required = false) String clientId,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) OffsetDateTime fromDate,
                                                        @RequestParam(required = false) OffsetDateTime toDate) {
        return ResponseEntity.ok(service.searchOrder(orderNumber, clientId, status, fromDate, toDate));
    }

    @GetMapping("/{orderNumber}/history/status")
    @Operation(
            summary = "Get order status history",
            description = "Retrieves the status history for a specific order using its unique orderNumber identifier."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order history found successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<List<OrderStatusHistoryDTO>> getOrderHistory(@PathVariable String orderNumber) {
        return ResponseEntity.ok(service.getOrderHistory(orderNumber));
    }

    @PostMapping
    @Operation(
            summary = "Create a new order",
            description = "Registers a new order in the system with a unique orderNumber."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or duplicated orderNumber")
    })
    public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) {
        OrderDTO created = service.createOrder(orderDTO);
        URI location = URI.create("/api/orders/" + created.getOrderNumber());
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/copy/{orderNumber}")
    @Operation(
            summary = "Create new order by order registered",
            description = "Registers a new order in the system by order registered."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or duplicated orderNumber")
    })
    public ResponseEntity<OrderDTO> copyOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(service.copyOrderCreated(orderNumber));
    }


    @PatchMapping("/{orderNumber}/change/status")
    @Operation(
            summary = "Change order status",
            description = "Updates the status of an existing order."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid order status or transition"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderDTO> changeOrderStatus(@PathVariable String orderNumber,
                                                      @RequestParam(required = false) OrderStatus newStatus,
                                                      @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(service.changeOrderStatus(orderNumber, newStatus, reason));
    }
}