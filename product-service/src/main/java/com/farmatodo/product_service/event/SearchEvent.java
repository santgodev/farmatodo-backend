package com.farmatodo.product_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Event object for product searches
 * Serializable to support future message queue implementations (e.g., GCP Pub/Sub)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String searchTerm;
    private Integer resultsCount;
    private String userIdentifier;
    private String transactionId;
    private LocalDateTime searchedAt;
}
