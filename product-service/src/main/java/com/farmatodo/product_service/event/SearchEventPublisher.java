package com.farmatodo.product_service.event;

/**
 * Abstraction for publishing search events
 * Current implementation: Spring @Async with ApplicationEventPublisher
 * Future: Can be replaced with GCP Pub/Sub, Kafka, RabbitMQ, etc.
 */
public interface SearchEventPublisher {

    /**
     * Publishes a search event asynchronously
     * @param event The search event to publish
     */
    void publishSearchEvent(SearchEvent event);
}
