package com.challange.order_service.mapper;

import com.challange.order_service.Enum.OrderStatus;
import com.challange.order_service.dto.OrderDTO;
import com.challange.order_service.dto.OrderDetailDTO;
import com.challange.order_service.dto.OrderStatusHistoryDTO;
import com.challange.order_service.entity.OrderDetailEntity;
import com.challange.order_service.entity.OrderEntity;
import com.challange.order_service.entity.OrderStatusHistoryEntity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Mapper {

    //? Order
    public static OrderDTO toDTO(OrderEntity o) {
        if (o == null) return null;

        var details = o.getDetails().stream().map(ord ->
                OrderDetailDTO.builder()
                        .productSku(ord.getProductSku())
                        .productName(ord.getProductName())
                        .quantity(ord.getQuantity())
                        .unitPrice(ord.getUnitPrice())
                        .subtotal(ord.getSubtotal())
                        .orderId(o.getId())
                        .build()
        ).toList();

        var history = o.getStatusHistory().stream().map(his ->
                OrderStatusHistoryDTO.builder()
                        .lastStatus(his.getLastStatus())
                        .newStatus(his.getNewStatus())
                        .date(his.getDate())
                        .reason(his.getReason())
                        .orderId(o.getId())
                        .build()
        ).toList();

        return OrderDTO.builder()
                .orderNumber(o.getOrderNumber())
                .clientId(o.getClientId())
                .status(o.getStatus())
                .total(o.getTotal())
                .createdDate(o.getCreatedDate())
                .updatedDate(o.getUpdatedDate())
                .version(o.getVersion())
                .details(details)
                .statusHistory(history)
                .build();
    }

    public static OrderEntity toEntity(OrderDTO o) {
        if (o == null) return null;

        OrderEntity ord = new OrderEntity();
        ord.setOrderNumber(o.getOrderNumber());
        ord.setClientId(o.getClientId());

        OrderStatus initialStatus = o.getStatus() != null ? o.getStatus() : OrderStatus.PENDING;
        ord.setStatus(initialStatus);

        var details = o.getDetails() != null ? o.getDetails().stream().map(det -> {
            BigDecimal subtotal = (det.getUnitPrice() != null && det.getQuantity() != null)
                    ? det.getUnitPrice().multiply(BigDecimal.valueOf(det.getQuantity()))
                    : BigDecimal.ZERO;

            return OrderDetailEntity.builder()
                    .productSku(det.getProductSku())
                    .productName(det.getProductName())
                    .quantity(det.getQuantity())
                    .unitPrice(det.getUnitPrice())
                    .subtotal(subtotal)
                    .order(ord)
                    .build();
        }).collect(Collectors.toList()) : new ArrayList<OrderDetailEntity>();

        BigDecimal total = details.stream()
                .map(OrderDetailEntity::getSubtotal)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<OrderStatusHistoryEntity> history = new ArrayList<>();
        history.add(OrderStatusHistoryEntity.builder()
                .lastStatus(null)
                .newStatus(initialStatus)
                .reason("Order has been created with the status pending")
                .order(ord)
                .build());

        ord.setTotal(total);
        ord.setDetails(details);
        ord.setStatusHistory(history);

        return ord;
    }

    //? Order History Status
    public static OrderStatusHistoryDTO toHistoryDTO(OrderStatusHistoryEntity his) {
        if (his == null) return null;
        return OrderStatusHistoryDTO.builder()
                .lastStatus(his.getLastStatus())
                .newStatus(his.getNewStatus())
                .date(his.getDate())
                .reason(his.getReason())
                .orderId(his.getOrder() != null ? his.getOrder().getId() : null)
                .build();
    }
}