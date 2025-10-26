package com.farmatodo.client_service.controller;

import com.farmatodo.client_service.dto.ClientRequestDTO;
import com.farmatodo.client_service.dto.ClientResponseDTO;
import com.farmatodo.client_service.dto.ClientUpdateDTO;
import com.farmatodo.client_service.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private static final Logger logger = LoggerFactory.getLogger(ClientController.class);

    private final ClientService clientService;

    @PostMapping
    public ResponseEntity<ClientResponseDTO> createClient(@RequestBody ClientRequestDTO request) {
        logger.info("Create client endpoint called, transaction: {}", MDC.get("transactionId"));

        ClientResponseDTO response = clientService.createClient(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> getClientById(@PathVariable Long id) {
        logger.info("Get client by id endpoint called, transaction: {}", MDC.get("transactionId"));

        ClientResponseDTO response = clientService.getClientById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<ClientResponseDTO> getClientByEmail(@PathVariable String email) {
        logger.info("Get client by email endpoint called, transaction: {}", MDC.get("transactionId"));

        ClientResponseDTO response = clientService.getClientByEmail(email);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ClientResponseDTO>> getAllClients() {
        logger.info("Get all clients endpoint called, transaction: {}", MDC.get("transactionId"));

        List<ClientResponseDTO> response = clientService.getAllClients();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDTO> updateClient(
            @PathVariable Long id,
            @RequestBody ClientUpdateDTO request) {
        logger.info("Update client endpoint called, transaction: {}", MDC.get("transactionId"));

        ClientResponseDTO response = clientService.updateClient(id, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        logger.info("Delete client endpoint called, transaction: {}", MDC.get("transactionId"));

        clientService.deleteClient(id);

        return ResponseEntity.noContent().build();
    }
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
