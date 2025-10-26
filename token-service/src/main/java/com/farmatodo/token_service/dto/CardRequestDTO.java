package com.farmatodo.token_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardRequestDTO {
    private String cardNumber;
    private String cvv;
    private String expiry;
    private String cardholderName;
    private Double rejectionProbability; // Optional: Override default rejection probability (0.0 to 1.0)
}
