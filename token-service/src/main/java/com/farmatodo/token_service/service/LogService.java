package com.farmatodo.token_service.service;

import com.farmatodo.token_service.model.LogEntry;
import com.farmatodo.token_service.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    private final LogRepository logRepository;

    @Value("${spring.application.name}")
    private String serviceName;

    /**
     * Log an INFO event to the centralized log database
     */
    @Async
    public void logInfo(String message, String additionalData) {
        saveLog("INFO", message, additionalData);
    }

    /**
     * Log a WARN event to the centralized log database
     */
    @Async
    public void logWarn(String message, String additionalData) {
        saveLog("WARN", message, additionalData);
    }

    /**
     * Log an ERROR event to the centralized log database
     */
    @Async
    public void logError(String message, String additionalData) {
        saveLog("ERROR", message, additionalData);
    }

    /**
     * Save log entry to database
     */
    private void saveLog(String eventType, String message, String additionalData) {
        try {
            String transactionId = MDC.get("transactionId");
            if (transactionId == null) {
                transactionId = "UNKNOWN";
            }

            LogEntry logEntry = LogEntry.builder()
                    .transactionId(transactionId)
                    .timestamp(LocalDateTime.now())
                    .serviceName(serviceName)
                    .eventType(eventType)
                    .message(message)
                    .additionalData(additionalData)
                    .build();

            logRepository.save(logEntry);
        } catch (Exception e) {
            // Don't let logging failures affect the main application flow
            logger.error("Failed to save log entry to database: {}", e.getMessage(), e);
        }
    }
}
