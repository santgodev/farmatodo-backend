package com.farmatodo.token_service.repository;

import com.farmatodo.token_service.model.TokenizedCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<TokenizedCard, Long> {
    Optional<TokenizedCard> findByToken(String token);
}
