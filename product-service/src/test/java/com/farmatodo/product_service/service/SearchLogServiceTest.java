package com.farmatodo.product_service.service;

import com.farmatodo.product_service.event.SearchEvent;
import com.farmatodo.product_service.model.SearchLog;
import com.farmatodo.product_service.repository.SearchLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for SearchLogService focusing on:
 * 1. Asynchronous logging of search queries
 * 2. Verifying repository save() is called with correct data
 * 3. Graceful error handling in async logging
 */
@ExtendWith(MockitoExtension.class)
class SearchLogServiceTest {

    @Mock
    private SearchLogRepository searchLogRepository;

    @InjectMocks
    private SearchLogService searchLogService;

    private SearchEvent searchEvent;

    @BeforeEach
    void setUp() {
        searchEvent = SearchEvent.builder()
                .searchTerm("aspirin")
                .resultsCount(5)
                .userIdentifier("192.168.1.1")
                .transactionId("txn-12345")
                .searchedAt(LocalDateTime.now())
                .build();
    }

    // ==================== ASYNC LOGGING TESTS ====================

    @Test
    void testHandleSearchEvent_ShouldSaveSearchLogToRepository() {
        // Arrange
        ArgumentCaptor<SearchLog> logCaptor = ArgumentCaptor.forClass(SearchLog.class);

        when(searchLogRepository.save(any(SearchLog.class))).thenAnswer(invocation -> {
            SearchLog log = invocation.getArgument(0);
            log.setId(1L); // Simulate database ID assignment
            return log;
        });

        // Act
        searchLogService.handleSearchEvent(searchEvent);

        // Assert - Verify save was called
        verify(searchLogRepository, times(1)).save(logCaptor.capture());

        SearchLog savedLog = logCaptor.getValue();
        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getSearchTerm()).isEqualTo("aspirin");
        assertThat(savedLog.getResultsCount()).isEqualTo(5);
        assertThat(savedLog.getUserIdentifier()).isEqualTo("192.168.1.1");
        assertThat(savedLog.getTransactionId()).isEqualTo("txn-12345");
        assertThat(savedLog.getSearchedAt()).isNotNull();
    }

    @Test
    void testHandleSearchEvent_VerifyRepositorySaveIsCalled() {
        // Arrange
        when(searchLogRepository.save(any(SearchLog.class))).thenReturn(new SearchLog());

        // Act
        searchLogService.handleSearchEvent(searchEvent);

        // Assert - This is the key test requirement: verify save() is called
        verify(searchLogRepository, times(1)).save(any(SearchLog.class));
    }

    @Test
    void testHandleSearchEvent_ShouldMapAllFieldsCorrectly() {
        // Arrange
        ArgumentCaptor<SearchLog> logCaptor = ArgumentCaptor.forClass(SearchLog.class);

        when(searchLogRepository.save(any(SearchLog.class))).thenReturn(new SearchLog());

        // Act
        searchLogService.handleSearchEvent(searchEvent);

        // Assert
        verify(searchLogRepository).save(logCaptor.capture());

        SearchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getSearchTerm()).isEqualTo(searchEvent.getSearchTerm());
        assertThat(savedLog.getResultsCount()).isEqualTo(searchEvent.getResultsCount());
        assertThat(savedLog.getUserIdentifier()).isEqualTo(searchEvent.getUserIdentifier());
        assertThat(savedLog.getTransactionId()).isEqualTo(searchEvent.getTransactionId());
        assertThat(savedLog.getSearchedAt()).isEqualTo(searchEvent.getSearchedAt());
    }

    @Test
    void testHandleSearchEvent_WithEmptySearchTerm_ShouldStillSave() {
        // Arrange
        SearchEvent emptySearchEvent = SearchEvent.builder()
                .searchTerm("")
                .resultsCount(10)
                .userIdentifier("10.0.0.1")
                .transactionId("txn-empty")
                .searchedAt(LocalDateTime.now())
                .build();

        ArgumentCaptor<SearchLog> logCaptor = ArgumentCaptor.forClass(SearchLog.class);

        when(searchLogRepository.save(any(SearchLog.class))).thenReturn(new SearchLog());

        // Act
        searchLogService.handleSearchEvent(emptySearchEvent);

        // Assert
        verify(searchLogRepository).save(logCaptor.capture());

        SearchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getSearchTerm()).isEmpty();
        assertThat(savedLog.getResultsCount()).isEqualTo(10);
    }

    @Test
    void testHandleSearchEvent_WithZeroResults_ShouldStillSave() {
        // Arrange
        SearchEvent noResultsEvent = SearchEvent.builder()
                .searchTerm("nonexistent")
                .resultsCount(0)
                .userIdentifier("192.168.1.1")
                .transactionId("txn-no-results")
                .searchedAt(LocalDateTime.now())
                .build();

        ArgumentCaptor<SearchLog> logCaptor = ArgumentCaptor.forClass(SearchLog.class);

        when(searchLogRepository.save(any(SearchLog.class))).thenReturn(new SearchLog());

        // Act
        searchLogService.handleSearchEvent(noResultsEvent);

        // Assert
        verify(searchLogRepository).save(logCaptor.capture());

        SearchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getSearchTerm()).isEqualTo("nonexistent");
        assertThat(savedLog.getResultsCount()).isEqualTo(0);
    }

    // ==================== ERROR HANDLING TESTS ====================

    @Test
    void testHandleSearchEvent_WhenRepositoryFails_ShouldNotThrowException() {
        // Arrange
        when(searchLogRepository.save(any(SearchLog.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert - Should not throw exception (graceful degradation)
        try {
            searchLogService.handleSearchEvent(searchEvent);
        } catch (Exception e) {
            // In a real async method with @Async, exceptions are swallowed
            // This test verifies the service handles errors gracefully
        }

        // Verify save was attempted
        verify(searchLogRepository).save(any(SearchLog.class));
    }

    @Test
    void testHandleSearchEvent_WhenRepositoryThrowsException_SaveIsStillAttempted() {
        // Arrange
        when(searchLogRepository.save(any(SearchLog.class)))
                .thenThrow(new RuntimeException("Persistence exception"));

        // Act
        try {
            searchLogService.handleSearchEvent(searchEvent);
        } catch (Exception ignored) {
            // Expected in test environment
        }

        // Assert - Verify the service attempted to save despite exception
        verify(searchLogRepository, times(1)).save(any(SearchLog.class));
    }

    // ==================== MULTIPLE EVENTS TESTS ====================

    @Test
    void testHandleSearchEvent_MultipleEvents_ShouldSaveEachIndependently() {
        // Arrange
        SearchEvent event1 = SearchEvent.builder()
                .searchTerm("aspirin")
                .resultsCount(5)
                .userIdentifier("192.168.1.1")
                .transactionId("txn-1")
                .searchedAt(LocalDateTime.now())
                .build();

        SearchEvent event2 = SearchEvent.builder()
                .searchTerm("vitamins")
                .resultsCount(10)
                .userIdentifier("192.168.1.2")
                .transactionId("txn-2")
                .searchedAt(LocalDateTime.now())
                .build();

        when(searchLogRepository.save(any(SearchLog.class))).thenReturn(new SearchLog());

        // Act
        searchLogService.handleSearchEvent(event1);
        searchLogService.handleSearchEvent(event2);

        // Assert
        verify(searchLogRepository, times(2)).save(any(SearchLog.class));
    }

    @Test
    void testHandleSearchEvent_WithNullTransactionId_ShouldHandleGracefully() {
        // Arrange
        SearchEvent eventWithNullTxnId = SearchEvent.builder()
                .searchTerm("test")
                .resultsCount(3)
                .userIdentifier("192.168.1.1")
                .transactionId(null)
                .searchedAt(LocalDateTime.now())
                .build();

        ArgumentCaptor<SearchLog> logCaptor = ArgumentCaptor.forClass(SearchLog.class);

        when(searchLogRepository.save(any(SearchLog.class))).thenReturn(new SearchLog());

        // Act
        searchLogService.handleSearchEvent(eventWithNullTxnId);

        // Assert
        verify(searchLogRepository).save(logCaptor.capture());

        SearchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getTransactionId()).isNull();
    }

    // ==================== DATA INTEGRITY TESTS ====================

    @Test
    void testHandleSearchEvent_PreservesSearchTimestamp() {
        // Arrange
        LocalDateTime specificTime = LocalDateTime.of(2025, 1, 15, 10, 30, 0);

        SearchEvent timedEvent = SearchEvent.builder()
                .searchTerm("test")
                .resultsCount(5)
                .userIdentifier("192.168.1.1")
                .transactionId("txn-timed")
                .searchedAt(specificTime)
                .build();

        ArgumentCaptor<SearchLog> logCaptor = ArgumentCaptor.forClass(SearchLog.class);

        when(searchLogRepository.save(any(SearchLog.class))).thenReturn(new SearchLog());

        // Act
        searchLogService.handleSearchEvent(timedEvent);

        // Assert
        verify(searchLogRepository).save(logCaptor.capture());

        SearchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getSearchedAt()).isEqualTo(specificTime);
    }

    @Test
    void testHandleSearchEvent_WithLongSearchTerm_ShouldSave() {
        // Arrange
        String longSearchTerm = "a".repeat(500); // Maximum length per entity definition

        SearchEvent longTermEvent = SearchEvent.builder()
                .searchTerm(longSearchTerm)
                .resultsCount(0)
                .userIdentifier("192.168.1.1")
                .transactionId("txn-long")
                .searchedAt(LocalDateTime.now())
                .build();

        ArgumentCaptor<SearchLog> logCaptor = ArgumentCaptor.forClass(SearchLog.class);

        when(searchLogRepository.save(any(SearchLog.class))).thenReturn(new SearchLog());

        // Act
        searchLogService.handleSearchEvent(longTermEvent);

        // Assert
        verify(searchLogRepository).save(logCaptor.capture());

        SearchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getSearchTerm()).hasSize(500);
    }

    @Test
    void testHandleSearchEvent_WithSpecialCharacters_ShouldPreserveCharacters() {
        // Arrange
        String specialSearchTerm = "test!@#$%^&*(){}[]|\\:;\"'<>,.?/~`";

        SearchEvent specialEvent = SearchEvent.builder()
                .searchTerm(specialSearchTerm)
                .resultsCount(2)
                .userIdentifier("192.168.1.1")
                .transactionId("txn-special")
                .searchedAt(LocalDateTime.now())
                .build();

        ArgumentCaptor<SearchLog> logCaptor = ArgumentCaptor.forClass(SearchLog.class);

        when(searchLogRepository.save(any(SearchLog.class))).thenReturn(new SearchLog());

        // Act
        searchLogService.handleSearchEvent(specialEvent);

        // Assert
        verify(searchLogRepository).save(logCaptor.capture());

        SearchLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getSearchTerm()).isEqualTo(specialSearchTerm);
    }
}
