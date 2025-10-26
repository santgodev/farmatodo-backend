package com.farmatodo.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {
    private Long orderId;
    private Long clientId;
    private String token;
    private List<OrderItemResponseDTO> items;
    private BigDecimal totalAmount;
    private String status;
    private String transactionId;
    private String rejectionReason;
    private Integer paymentAttempts;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
