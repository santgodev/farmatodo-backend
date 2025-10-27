package com.farmatodo.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequestDTO {
    private Long orderId; // Optional: If provided, will be used as the order ID
    private Long clientId;
    private String token;
    private String email;
    private Double rejectionProbability; // Optional: Override payment rejection probability (0.0 to 1.0)
    private Integer maxAttempts; // Optional: Override payment retry attempts (default: 3)
}
