package com.farmatodo.product_service.event.impl;

import com.farmatodo.product_service.event.SearchEvent;
import com.farmatodo.product_service.event.SearchEventPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Local implementation using Spring's ApplicationEventPublisher
 * This is the default implementation for non-GCP environments
 *
 * To switch to GCP Pub/Sub:
 * 1. Create GcpPubSubSearchEventPublisher implements SearchEventPublisher
 * 2. Annotate it with @Component and @Profile("gcp")
 * 3. Add @Profile("!gcp") to this class
 */
@Component
@RequiredArgsConstructor
public class LocalSearchEventPublisher implements SearchEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(LocalSearchEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void publishSearchEvent(SearchEvent event) {
        logger.debug("Publishing search event locally: {}", event.getSearchTerm());
        applicationEventPublisher.publishEvent(event);
    }
}
