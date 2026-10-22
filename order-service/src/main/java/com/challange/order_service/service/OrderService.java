package com.challange.order_service.service;

import com.challange.order_service.Enum.OrderStatus;
import com.challange.order_service.client.InventoryClient;
import com.challange.order_service.dto.OrderDTO;
import com.challange.order_service.dto.OrderStatusHistoryDTO;
import com.challange.order_service.entity.OrderEntity;
import com.challange.order_service.entity.OrderStatusHistoryEntity;
import com.challange.order_service.exception.NotFoundException;
import com.challange.order_service.mapper.Mapper;
import com.challange.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {

    private final OrderRepository repository;
    private final InventoryClient client;

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrder(String orderNumber) {

        log.info("Searching order with number: {}", orderNumber);

        return Mapper.toDTO(repository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> {
                    log.warn("Order not found with number: {}", orderNumber);
                    return new NotFoundException("Order not found with order number: " + orderNumber);
                }));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {

        log.info("Querying all registered orders");

        return repository.findAll().stream().map(Mapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> searchOrder(String orderNumber, String clientId, String status, OffsetDateTime fromDate, OffsetDateTime toDate) {

        log.info("Searching orders with advanced filters - orderNumber: {}, clientId: {}, status: {}", orderNumber, clientId, status);

        return repository.searchOrder(orderNumber, clientId, status, fromDate, toDate)
                .stream().map(Mapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderStatusHistoryDTO> getOrderHistory(String orderNumber) {

        log.info("Querying status history for order: {}", orderNumber);

        OrderEntity ord = repository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> {
                    log.warn("Could not retrieve history. Order not found: {}", orderNumber);
                    return new NotFoundException("Order not found with order number: " + orderNumber);
                });

        return ord.getStatusHistory().stream().map(Mapper::toHistoryDTO).toList();
    }

    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO order) {

        log.info("Initiating creation process for order: {}", order.getOrderNumber());

        if (repository.existsByOrderNumber(order.getOrderNumber())) {
            log.warn("Attempt to create a duplicate order with number: {}", order.getOrderNumber());
            throw new IllegalArgumentException("The order with the order number " + order.getOrderNumber() + " already exists");
        }

        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        assert jwtAuth != null;
        String currentUserId = jwtAuth.getToken().getSubject();
        log.debug("Authenticated user detected for order creation: {}", currentUserId);

        OrderEntity ord = Mapper.toEntity(order);
        ord.setClientId(currentUserId);

        OrderEntity savedOrder = repository.save(ord);
        log.info("Order successfully created with number: {}", savedOrder.getOrderNumber());

        return Mapper.toDTO(savedOrder);
    }

    @Override
    public OrderDTO copyOrderCreated(String orderNumber) {

        log.info("Initiating copy process for order: {}", orderNumber);

        OrderEntity oldOrder = repository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> {
                    log.warn("Order not found for status change: {}", orderNumber);
                    return new NotFoundException("Order not found with order number: " + orderNumber);
                });

        String timeMs = String.valueOf(LocalDateTime.now().getNano());

        OrderDTO newOrder = Mapper.toDTO(oldOrder);
        newOrder.setOrderNumber(orderNumber + timeMs);
        newOrder.setStatus(OrderStatus.PENDING);
        OrderEntity changedOrder = Mapper.toEntity(newOrder);

        repository.save(changedOrder);
        log.info("New Order successfully created with number: {}", changedOrder.getOrderNumber());

        return Mapper.toDTO(changedOrder);
    }

    @Override
    @Transactional
    public OrderDTO changeOrderStatus(String orderNumber, OrderStatus newStatus, String reason) {

        log.info("Request to change status of order {} to {}. Reason: {}", orderNumber, newStatus, reason);

        OrderEntity order = repository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> {
                    log.warn("Order not found for status change: {}", orderNumber);
                    return new NotFoundException("Order not found with order number: " + orderNumber);
                });

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == newStatus) {
            log.info("Order {} is already in the desired status: {}", orderNumber, newStatus);
            return Mapper.toDTO(order);
        }

        if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.FAILED) {
            log.error("Invalid attempt to change status of order {}. Current status is terminal: {}", orderNumber, currentStatus);
            throw new IllegalStateException("Cannot change status of an order that is already " + currentStatus);
        }

        handleStockSynchronization(order, currentStatus, newStatus);

        order.setStatus(newStatus);

        OrderStatusHistoryEntity history = OrderStatusHistoryEntity.builder()
                .lastStatus(currentStatus)
                .newStatus(newStatus)
                .reason(reason != null ? reason : "Status changed to " + newStatus)
                .order(order)
                .build();

        order.getStatusHistory().add(history);
        log.info("Order status for {} successfully updated from {} to {}", orderNumber, currentStatus, newStatus);

        return Mapper.toDTO(order);
    }

    private void handleStockSynchronization(OrderEntity order, OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == OrderStatus.PENDING && newStatus == OrderStatus.CONFIRMED) {
            log.info("Stock synchronization: Subtracting inventory (SUBTRACT) for products in order {}", order.getOrderNumber());
            order.getDetails().forEach(detail -> {
                log.debug("Subtracting {} units of product SKU: {}", detail.getQuantity(), detail.getProductSku());
                client.updateStockProduct(detail.getProductSku(), detail.getQuantity(), "SUBTRACT");
            });
        }
        else if ((newStatus == OrderStatus.CANCELLED && currentStatus != OrderStatus.PENDING) ||
                (newStatus == OrderStatus.FAILED && currentStatus == OrderStatus.CONFIRMED)) {
            log.info("Stock synchronization: Returning inventory (ADD) for products in {} order {}", newStatus.name().toLowerCase(), order.getOrderNumber());
            order.getDetails().forEach(detail -> {
                log.debug("Adding {} units of product SKU: {}", detail.getQuantity(), detail.getProductSku());
                client.updateStockProduct(detail.getProductSku(), detail.getQuantity(), "ADD");
            });
        }
    }
}