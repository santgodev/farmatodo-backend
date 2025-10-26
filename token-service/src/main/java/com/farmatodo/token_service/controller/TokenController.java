package com.farmatodo.token_service.controller;

import com.farmatodo.token_service.dto.CardRequestDTO;
import com.farmatodo.token_service.dto.TokenResponseDTO;
import com.farmatodo.token_service.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class TokenController {

    private static final Logger logger = LoggerFactory.getLogger(TokenController.class);

    private final TokenService tokenService;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        logger.info("Ping endpoint called, transaction: {}", MDC.get("transactionId"));
        return ResponseEntity.ok("pong");
    }

    @PostMapping("/tokenize")
    public ResponseEntity<TokenResponseDTO> tokenize(@RequestBody CardRequestDTO request) {
        logger.info("Tokenize endpoint called, transaction: {}", MDC.get("transactionId"));

        TokenResponseDTO response = tokenService.tokenize(request);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}
