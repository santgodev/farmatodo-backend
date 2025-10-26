package com.farmatodo.client_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientRequestDTO {
    private String name;
    private String email;
    private String phone;
    private String address;
    private String documentType;
    private String documentNumber;
}
