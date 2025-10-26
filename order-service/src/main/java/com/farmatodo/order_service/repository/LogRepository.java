package com.farmatodo.order_service.repository;

import com.farmatodo.order_service.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<LogEntry, Long> {
    List<LogEntry> findByTransactionIdOrderByTimestampAsc(String transactionId);
    List<LogEntry> findByServiceNameOrderByTimestampDesc(String serviceName);
}
