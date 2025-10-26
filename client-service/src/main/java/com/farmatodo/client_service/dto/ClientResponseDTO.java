package com.farmatodo.client_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponseDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String documentType;
    private String documentNumber;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
