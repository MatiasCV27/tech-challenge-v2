package com.challange.order_service.service;

import com.challange.order_service.Enum.OrderStatus;
import com.challange.order_service.dto.OrderDTO;
import com.challange.order_service.dto.OrderStatusHistoryDTO;

import java.time.OffsetDateTime;
import java.util.List;

public interface IOrderService {

    OrderDTO getOrder(String orderNumber);
    List<OrderDTO> getAllOrders();
    List<OrderDTO> searchOrder(String orderNumber, String clientId, String status, OffsetDateTime fromDate, OffsetDateTime toDate);
    List<OrderStatusHistoryDTO> getOrderHistory(String orderNumber);
    OrderDTO createOrder(OrderDTO orderDTO);
    OrderDTO copyOrderCreated(String orderNumber);
    OrderDTO changeOrderStatus(String orderNumber, OrderStatus newStatus, String reason);


}