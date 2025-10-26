package com.farmatodo.client_service.service;

import com.farmatodo.client_service.dto.ClientRequestDTO;
import com.farmatodo.client_service.dto.ClientResponseDTO;
import com.farmatodo.client_service.dto.ClientUpdateDTO;
import com.farmatodo.client_service.exception.BusinessException;
import com.farmatodo.client_service.model.Client;
import com.farmatodo.client_service.repository.ClientRepository;
import com.farmatodo.client_service.util.ClientValidator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private static final Logger logger = LoggerFactory.getLogger(ClientService.class);

    private final ClientRepository clientRepository;
    private final ClientValidator clientValidator;

    @Transactional
    public ClientResponseDTO createClient(ClientRequestDTO request) {
        String transactionId = MDC.get("transactionId");
        logger.info("Starting client creation for transaction: {}", transactionId);

        // Validate all fields
        clientValidator.validateName(request.getName());
        clientValidator.validateEmail(request.getEmail());
        clientValidator.validatePhone(request.getPhone());
        clientValidator.validateAddress(request.getAddress());
        clientValidator.validateDocumentType(request.getDocumentType());
        clientValidator.validateDocumentNumber(request.getDocumentNumber());

        logger.debug("Client validation passed for transaction: {}", transactionId);

        // Normalize data
        String normalizedEmail = clientValidator.normalizeEmail(request.getEmail());
        String cleanPhone = clientValidator.cleanPhone(request.getPhone());
        String normalizedDocType = clientValidator.normalizeDocumentType(request.getDocumentType());

        // Check uniqueness
        if (clientRepository.existsByEmail(normalizedEmail)) {
            logger.warn("Email already exists for transaction: {}", transactionId);
            throw new BusinessException(
                    "Email already registered",
                    "EMAIL_ALREADY_EXISTS",
                    409
            );
        }

        if (clientRepository.existsByPhone(cleanPhone)) {
            logger.warn("Phone already exists for transaction: {}", transactionId);
            throw new BusinessException(
                    "Phone already registered",
                    "PHONE_ALREADY_EXISTS",
                    409
            );
        }

        if (clientRepository.existsByDocumentTypeAndDocumentNumber(
                normalizedDocType, request.getDocumentNumber().trim())) {
            logger.warn("Document already exists for transaction: {}", transactionId);
            throw new BusinessException(
                    "Document already registered",
                    "DOCUMENT_ALREADY_EXISTS",
                    409
            );
        }

        // Create client
        Client client = Client.builder()
                .name(request.getName().trim())
                .email(normalizedEmail)
                .phone(cleanPhone)
                .address(request.getAddress().trim())
                .documentType(normalizedDocType)
                .documentNumber(request.getDocumentNumber().trim())
                .status("ACTIVE")
                .build();

        Client savedClient = clientRepository.save(client);

        logger.info("Client created successfully for transaction: {}, clientId: {}",
                transactionId, savedClient.getId());

        return mapToResponseDTO(savedClient);
    }

    @Transactional(readOnly = true)
    public ClientResponseDTO getClientById(Long id) {
        String transactionId = MDC.get("transactionId");
        logger.info("Retrieving client by id: {} for transaction: {}", id, transactionId);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Client not found with id: " + id,
                        "CLIENT_NOT_FOUND",
                        404
                ));

        return mapToResponseDTO(client);
    }

    @Transactional(readOnly = true)
    public ClientResponseDTO getClientByEmail(String email) {
        String transactionId = MDC.get("transactionId");
        logger.info("Retrieving client by email for transaction: {}", transactionId);

        String normalizedEmail = clientValidator.normalizeEmail(email);

        Client client = clientRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(
                        "Client not found with email: " + email,
                        "CLIENT_NOT_FOUND",
                        404
                ));

        return mapToResponseDTO(client);
    }

    @Transactional(readOnly = true)
    public List<ClientResponseDTO> getAllClients() {
        String transactionId = MDC.get("transactionId");
        logger.info("Retrieving all clients for transaction: {}", transactionId);

        return clientRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClientResponseDTO updateClient(Long id, ClientUpdateDTO request) {
        String transactionId = MDC.get("transactionId");
        logger.info("Updating client id: {} for transaction: {}", id, transactionId);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Client not found with id: " + id,
                        "CLIENT_NOT_FOUND",
                        404
                ));

        // Update fields if provided
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            clientValidator.validateName(request.getName());
            client.setName(request.getName().trim());
        }

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            clientValidator.validatePhone(request.getPhone());
            String cleanPhone = clientValidator.cleanPhone(request.getPhone());

            // Check if new phone is different and not already used
            if (!cleanPhone.equals(client.getPhone())) {
                if (clientRepository.existsByPhone(cleanPhone)) {
                    throw new BusinessException(
                            "Phone already registered",
                            "PHONE_ALREADY_EXISTS",
                            409
                    );
                }
                client.setPhone(cleanPhone);
            }
        }

        if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
            clientValidator.validateAddress(request.getAddress());
            client.setAddress(request.getAddress().trim());
        }

        Client updatedClient = clientRepository.save(client);

        logger.info("Client updated successfully for transaction: {}, clientId: {}",
                transactionId, updatedClient.getId());

        return mapToResponseDTO(updatedClient);
    }

    @Transactional
    public void deleteClient(Long id) {
        String transactionId = MDC.get("transactionId");
        logger.info("Deleting client id: {} for transaction: {}", id, transactionId);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "Client not found with id: " + id,
                        "CLIENT_NOT_FOUND",
                        404
                ));

        // Soft delete by changing status
        client.setStatus("INACTIVE");
        clientRepository.save(client);

        logger.info("Client deleted (soft) successfully for transaction: {}, clientId: {}",
                transactionId, id);
    }

    private ClientResponseDTO mapToResponseDTO(Client client) {
        return ClientResponseDTO.builder()
                .id(client.getId())
                .name(client.getName())
                .email(client.getEmail())
                .phone(client.getPhone())
                .address(client.getAddress())
                .documentType(client.getDocumentType())
                .documentNumber(client.getDocumentNumber())
                .status(client.getStatus())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }
}
