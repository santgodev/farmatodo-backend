package com.farmatodo.order_service.client;

import com.farmatodo.order_service.dto.ClientDTO;
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
public class ClientServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(ClientServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${services.client.url}")
    private String clientServiceUrl;

    @Value("${services.client.apiKey}")
    private String apiKey;

    public ClientDTO getClientById(Long clientId) {
        try {
            String url = clientServiceUrl + "/clients/" + clientId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey " + apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            logger.info("Fetching client data from client-service for clientId: {}", clientId);

            ResponseEntity<ClientDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ClientDTO.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                logger.info("Successfully fetched client data for clientId: {}", clientId);
                return response.getBody();
            } else {
                throw new BusinessException(
                        "Failed to fetch client data",
                        "CLIENT_FETCH_FAILED",
                        500
                );
            }
        } catch (Exception e) {
            logger.error("Error fetching client data for clientId: {}", clientId, e);
            throw new BusinessException(
                    "Client service unavailable or client not found",
                    "CLIENT_SERVICE_ERROR",
                    500
            );
        }
    }
}
