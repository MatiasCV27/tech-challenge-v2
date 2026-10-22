package com.challange.order_service.dto;

import com.challange.order_service.Enum.OrderStatus;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderStatusHistoryDTO {

    private OrderStatus lastStatus;
    private OrderStatus newStatus;
    private OffsetDateTime date;
    private String reason;
    private Long orderId;
}
