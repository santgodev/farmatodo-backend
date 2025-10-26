package com.farmatodo.token_service.repository;

import com.farmatodo.token_service.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findByTransactionIdOrderByTimestampAsc(String transactionId);
    List<LogEntry> findByServiceNameOrderByTimestampDesc(String serviceName);
}
