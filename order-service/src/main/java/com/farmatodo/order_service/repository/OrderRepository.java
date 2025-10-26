package com.farmatodo.order_service.repository;

import com.farmatodo.order_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByClientIdOrderByCreatedAtDesc(Long clientId);
    Optional<Order> findByTransactionId(String transactionId);
    List<Order> findByStatus(String status);
}
