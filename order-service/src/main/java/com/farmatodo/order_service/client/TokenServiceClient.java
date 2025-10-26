package com.farmatodo.order_service.client;

import com.farmatodo.order_service.dto.PaymentRequestDTO;
import com.farmatodo.order_service.dto.PaymentResponseDTO;
import com.farmatodo.order_service.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class TokenServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(TokenServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${services.token.url}")
    private String tokenServiceUrl;

    @Value("${services.token.apiKey}")
    private String apiKey;

    public PaymentResponseDTO processPayment(PaymentRequestDTO paymentRequest) {
        try {
            String url = tokenServiceUrl + "/api/tokens/payment";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey " + apiKey);
            headers.set("Content-Type", "application/json");
            HttpEntity<PaymentRequestDTO> entity = new HttpEntity<>(paymentRequest, headers);

            logger.info("Processing payment with token-service for orderId: {}", paymentRequest.getOrderId());

            ResponseEntity<PaymentResponseDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PaymentResponseDTO.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Payment processing completed for orderId: {}", paymentRequest.getOrderId());
                return response.getBody();
            } else {
                throw new BusinessException(
                        "Payment processing failed",
                        "PAYMENT_FAILED",
                        500
                );
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error processing payment for orderId: {}", paymentRequest.getOrderId(), e);
            throw new BusinessException(
                    "Token service unavailable",
                    "TOKEN_SERVICE_ERROR",
                    500
            );
        }
    }
}
