package com.challange.order_service.service;

import com.challange.order_service.Enum.OrderStatus;
import com.challange.order_service.client.InventoryClient;
import com.challange.order_service.dto.OrderDTO;
import com.challange.order_service.dto.OrderDetailDTO;
import com.challange.order_service.entity.OrderEntity;
import com.challange.order_service.exception.NotFoundException;
import com.challange.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private JwtAuthenticationToken jwtAuthenticationToken;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private OrderService orderService;

    private OrderDTO sampleOrderDTO;

    @BeforeEach
    void setUp() {
        lenient().when(securityContext.getAuthentication()).thenReturn(jwtAuthenticationToken);
        lenient().when(jwtAuthenticationToken.getToken()).thenReturn(jwt);
        lenient().when(jwt.getSubject()).thenReturn("client-id-123");
        SecurityContextHolder.setContext(securityContext);

        OrderDetailDTO detailDTO = new OrderDetailDTO();
        detailDTO.setProductSku("SKU-001");
        detailDTO.setProductName("Test Product");
        detailDTO.setQuantity(2);
        detailDTO.setUnitPrice(BigDecimal.valueOf(100.00));

        sampleOrderDTO = new OrderDTO();
        sampleOrderDTO.setOrderNumber("ORD-001");
        sampleOrderDTO.setDetails(List.of(detailDTO));
    }

    //? Create order test
    @Test
    @DisplayName("Should create order successfully and assign clientId from token")
    void createOrder_Success() {
        when(orderRepository.existsByOrderNumber("ORD-001")).thenReturn(false);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDTO result = orderService.createOrder(sampleOrderDTO);

        assertNotNull(result);
        assertEquals("ORD-001", result.getOrderNumber());
        assertEquals("client-id-123", result.getClientId());
        assertEquals(BigDecimal.valueOf(200.00), result.getTotal());

        verify(orderRepository, times(1)).existsByOrderNumber("ORD-001");
        verify(orderRepository, times(1)).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Should throw exception when order number already exists")
    void createOrder_AlreadyExists_ThrowsException() {
        when(orderRepository.existsByOrderNumber("ORD-001")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(sampleOrderDTO)
        );

        assertTrue(exception.getMessage().contains("already exists"));
        verify(orderRepository, times(1)).existsByOrderNumber("ORD-001");
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }

    //? Change order status test
    @Test
    @DisplayName("Should return order DTO when status is the same")
    void changeOrderStatus_SameStatus_ReturnsDTO() {
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-001");
        order.setStatus(OrderStatus.PENDING);
        order.setDetails(List.of());

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        OrderDTO result = orderService.changeOrderStatus("ORD-001", OrderStatus.PENDING, "Reason");

        assertNotNull(result);
        assertEquals("ORD-001", result.getOrderNumber());
        verify(orderRepository, never()).save(any());
        verify(inventoryClient, never()).updateStockProduct(any(), any(), any());
    }

    @Test
    @DisplayName("Should update status and subtract stock when changing from PENDING to CONFIRMED")
    void changeOrderStatus_PendingToConfirmed_SuccessAndSyncStock() {
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-001");
        order.setStatus(OrderStatus.PENDING);

        var detail = new com.challange.order_service.entity.OrderDetailEntity();
        detail.setProductSku("SKU-001");
        detail.setQuantity(3);
        order.setDetails(List.of(detail));
        order.setStatusHistory(new ArrayList<>());

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        OrderDTO result = orderService.changeOrderStatus("ORD-001", OrderStatus.CONFIRMED, "Confirming order");

        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(inventoryClient, times(1)).updateStockProduct("SKU-001", 3, "SUBTRACT");
    }

    @Test
    @DisplayName("Should update status and return stock when changing from CONFIRMED to CANCELLED")
    void changeOrderStatus_ConfirmedToCancelled_SuccessAndReturnStock() {
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-001");
        order.setStatus(OrderStatus.CONFIRMED);

        var detail = new com.challange.order_service.entity.OrderDetailEntity();
        detail.setProductSku("SKU-001");
        detail.setQuantity(2);
        order.setDetails(List.of(detail));
        order.setStatusHistory(new ArrayList<>());

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        OrderDTO result = orderService.changeOrderStatus("ORD-001", OrderStatus.CANCELLED, "Cancelling order");

        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(inventoryClient, times(1)).updateStockProduct("SKU-001", 2, "ADD");
    }

    @Test
    @DisplayName("Should update status and return stock when changing from CONFIRMED to FAILED")
    void changeOrderStatus_ConfirmedToFailed_SuccessAndReturnStock() {
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-001");
        order.setStatus(OrderStatus.CONFIRMED);

        var detail = new com.challange.order_service.entity.OrderDetailEntity();
        detail.setProductSku("SKU-001");
        detail.setQuantity(5);
        order.setDetails(List.of(detail));
        order.setStatusHistory(new ArrayList<>());

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        OrderDTO result = orderService.changeOrderStatus("ORD-001", OrderStatus.FAILED, "Order processing failed");

        assertNotNull(result);
        assertEquals(OrderStatus.FAILED, order.getStatus());
        verify(inventoryClient, times(1)).updateStockProduct("SKU-001", 5, "ADD");
    }

    @Test
    @DisplayName("Should throw NotFoundException when order does not exist")
    void changeOrderStatus_NotFound_ThrowsException() {
        when(orderRepository.findByOrderNumber("ORD-999")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> orderService.changeOrderStatus("ORD-999", OrderStatus.CONFIRMED, "Reason")
        );

        assertTrue(exception.getMessage().contains("Order not found"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when order is already CANCELLED")
    void changeOrderStatus_AlreadyCancelled_ThrowsException() {
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-001");
        order.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.changeOrderStatus("ORD-001", OrderStatus.CONFIRMED, "Reason")
        );

        assertTrue(exception.getMessage().contains("Cannot change status of an order that is already"));
        verify(inventoryClient, never()).updateStockProduct(any(), any(), any());
    }

    @Test
    @DisplayName("Should throw IllegalStateException when order is already FAILED (terminal status)")
    void changeOrderStatus_AlreadyFailed_ThrowsException() {
        OrderEntity order = new OrderEntity();
        order.setOrderNumber("ORD-001");
        order.setStatus(OrderStatus.FAILED);

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.changeOrderStatus("ORD-001", OrderStatus.CONFIRMED, "Reason")
        );

        assertTrue(exception.getMessage().contains("Cannot change status of an order that is already"));
        verify(inventoryClient, never()).updateStockProduct(any(), any(), any());
    }

    //? Validate copy order

    @Test
    @DisplayName("Should copy order successfully with a new order number and PENDING status")
    void copyOrderCreated_Success() {
        String originalOrderNumber = "ORD-001";

        OrderEntity existingOrderEntity = new OrderEntity();
        existingOrderEntity.setOrderNumber(originalOrderNumber);
        existingOrderEntity.setClientId("client-id-123");
        existingOrderEntity.setStatus(OrderStatus.CONFIRMED);
        existingOrderEntity.setTotal(BigDecimal.valueOf(200.00));

        when(orderRepository.findByOrderNumber(originalOrderNumber)).thenReturn(Optional.of(existingOrderEntity));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDTO result = orderService.copyOrderCreated(originalOrderNumber);

        assertNotNull(result);
        assertNotNull(result.getOrderNumber());
        assertTrue(result.getOrderNumber().startsWith(originalOrderNumber));
        assertNotEquals(originalOrderNumber, result.getOrderNumber());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals("client-id-123", result.getClientId());

        verify(orderRepository, times(1)).findByOrderNumber(originalOrderNumber);
        verify(orderRepository, times(1)).save(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Should throw NotFoundException when trying to copy a non-existent order")
    void copyOrderCreated_NotFound() {

        String nonExistentOrderNumber = "ORD-999";
        when(orderRepository.findByOrderNumber(nonExistentOrderNumber)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            orderService.copyOrderCreated(nonExistentOrderNumber);
        });

        assertEquals("Order not found with order number: " + nonExistentOrderNumber, exception.getMessage());

        verify(orderRepository, times(1)).findByOrderNumber(nonExistentOrderNumber);
        verify(orderRepository, never()).save(any(OrderEntity.class));
    }
}