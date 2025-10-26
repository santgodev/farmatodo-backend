package com.farmatodo.client_service.repository;

import com.farmatodo.client_service.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByEmail(String email);
    Optional<Client> findByPhone(String phone);
    Optional<Client> findByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByDocumentTypeAndDocumentNumber(String documentType, String documentNumber);
}
