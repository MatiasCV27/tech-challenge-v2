package com.challange.order_service.dto;

import com.challange.order_service.Enum.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDTO {

    private String orderNumber;
    private String clientId;
    private OrderStatus status;
    private BigDecimal total;
    private OffsetDateTime createdDate;
    private OffsetDateTime updatedDate;
    private Long version;

    private List<OrderDetailDTO> details;
    private List<OrderStatusHistoryDTO> statusHistory;
}
