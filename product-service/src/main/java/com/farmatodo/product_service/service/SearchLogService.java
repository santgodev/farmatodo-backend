package com.farmatodo.product_service.service;

import com.farmatodo.product_service.event.SearchEvent;
import com.farmatodo.product_service.model.SearchLog;
import com.farmatodo.product_service.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for logging search events asynchronously
 * Uses @Async to ensure HTTP responses are not delayed
 * Listens to SearchEvent published by LocalSearchEventPublisher
 */
@Service
@RequiredArgsConstructor
public class SearchLogService {

    private static final Logger logger = LoggerFactory.getLogger(SearchLogService.class);

    private final SearchLogRepository searchLogRepository;

    /**
     * Asynchronously handles search events and persists them to the database
     * Uses REQUIRES_NEW propagation to ensure independent transaction
     *
     * @param event The search event to log
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleSearchEvent(SearchEvent event) {
        try {
            logger.info("Logging search asynchronously - Term: '{}', Results: {}, Transaction: {}",
                    event.getSearchTerm(), event.getResultsCount(), event.getTransactionId());

            SearchLog searchLog = SearchLog.builder()
                    .searchTerm(event.getSearchTerm())
                    .resultsCount(event.getResultsCount())
                    .userIdentifier(event.getUserIdentifier())
                    .transactionId(event.getTransactionId())
                    .searchedAt(event.getSearchedAt())
                    .build();

            searchLogRepository.save(searchLog);

            logger.debug("Search log saved successfully for transaction: {}", event.getTransactionId());

        } catch (Exception e) {
            // Log error but don't fail - search logging is non-critical
            logger.error("Failed to save search log for transaction: {}. Error: {}",
                    event.getTransactionId(), e.getMessage(), e);
        }
    }

    /**
     * For GCP Pub/Sub implementation:
     * 1. Remove @EventListener annotation
     * 2. This method would be called by a Pub/Sub subscriber
     * 3. Create a separate GcpPubSubSubscriber class with:
     *    @MessageMapping or @PubSubListener
     *    that calls this service method
     */
}
