package com.farmatodo.token_service.controller;

import com.farmatodo.token_service.dto.PaymentRequestDTO;
import com.farmatodo.token_service.dto.PaymentResponseDTO;
import com.farmatodo.token_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    @PostMapping("/payment")
    public ResponseEntity<PaymentResponseDTO> processPayment(
            @RequestBody PaymentRequestDTO request,
            @RequestHeader(value = "X-Client-Email", required = false, defaultValue = "customer@example.com") String clientEmail) {

        String transactionId = MDC.get("transactionId");
        logger.info("Payment request received - TransactionId: {}, OrderId: {}", transactionId, request.getOrderId());

        PaymentResponseDTO response = paymentService.processPayment(request, clientEmail);

        if (response.isApproved()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(402).body(response); // 402 Payment Required
        }
    }
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
