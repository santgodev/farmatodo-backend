package com.farmatodo.product_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "search_logs", indexes = {
    @Index(name = "idx_search_term", columnList = "searchTerm"),
    @Index(name = "idx_searched_at", columnList = "searchedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String searchTerm;

    @Column(nullable = false)
    private Integer resultsCount;

    @Column(length = 100)
    private String userIdentifier; // Could be email, user ID, or IP address

    @Column(length = 50)
    private String transactionId;

    @Column(nullable = false)
    private LocalDateTime searchedAt;

    @PrePersist
    protected void onCreate() {
        searchedAt = LocalDateTime.now();
    }
}
