package com.farmatodo.token_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {
    private String token;
    private BigDecimal amount;
    private Long orderId;
    private Long clientId;
    private Double rejectionProbability; // Optional: Override default rejection probability (0.0 to 1.0)
    private Integer maxAttempts; // Optional: Override default retry attempts (default: 3)
}
