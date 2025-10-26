package com.farmatodo.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResponseDTO {
    private String query;
    private Integer totalResults;
    private List<ProductDTO> products;
}
