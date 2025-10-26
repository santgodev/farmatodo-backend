package com.farmatodo.client_service.controller;

import com.farmatodo.client_service.dto.ClientRequestDTO;
import com.farmatodo.client_service.dto.ClientResponseDTO;
import com.farmatodo.client_service.dto.ClientUpdateDTO;
import com.farmatodo.client_service.exception.BusinessException;
import com.farmatodo.client_service.service.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@TestPropertySource(properties = {
        "api.key=test-api-key-12345"
})
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ClientService clientService;

    private ClientRequestDTO validClientRequest;
    private ClientResponseDTO clientResponse;

    @BeforeEach
    void setUp() {
        validClientRequest = new ClientRequestDTO(
                "John Doe",
                "john.doe@example.com",
                "+1234567890",
                "123 Main Street, City",
                "DNI",
                "12345678"
        );

        clientResponse = ClientResponseDTO.builder()
                .id(1L)
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .address("123 Main Street, City")
                .documentType("DNI")
                .documentNumber("12345678")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== PING ENDPOINT TESTS ====================

    @Test
    void testPing_ShouldReturnPong() throws Exception {
        mockMvc.perform(get("/clients/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    // ==================== CREATE CLIENT - HAPPY PATH TESTS ====================

    @Test
    void testCreateClient_ValidData_ShouldReturnCreated() throws Exception {
        // Arrange
        when(clientService.createClient(any(ClientRequestDTO.class))).thenReturn(clientResponse);

        // Act & Assert
        mockMvc.perform(post("/clients")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validClientRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.phone").value("+1234567890"))
                .andExpect(jsonPath("$.address").value("123 Main Street, City"))
                .andExpect(jsonPath("$.documentType").value("DNI"))
                .andExpect(jsonPath("$.documentNumber").value("12345678"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(clientService, times(1)).createClient(any(ClientRequestDTO.class));
    }

    // ==================== CREATE CLIENT - VALIDATION TESTS ====================

    @Test
    void testCreateClient_DuplicateEmail_ShouldReturnConflict() throws Exception {
        // Arrange
        when(clientService.createClient(any(ClientRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Email already registered",
                        "EMAIL_ALREADY_EXISTS",
                        409
                ));

        // Act & Assert
        mockMvc.perform(post("/clients")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validClientRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Email already registered"));

        verify(clientService, times(1)).createClient(any(ClientRequestDTO.class));
    }

    @Test
    void testCreateClient_DuplicatePhone_ShouldReturnConflict() throws Exception {
        // Arrange
        when(clientService.createClient(any(ClientRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Phone already registered",
                        "PHONE_ALREADY_EXISTS",
                        409
                ));

        // Act & Assert
        mockMvc.perform(post("/clients")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validClientRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PHONE_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Phone already registered"));
    }

    @Test
    void testCreateClient_DuplicateDocument_ShouldReturnConflict() throws Exception {
        // Arrange
        when(clientService.createClient(any(ClientRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Document already registered",
                        "DOCUMENT_ALREADY_EXISTS",
                        409
                ));

        // Act & Assert
        mockMvc.perform(post("/clients")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validClientRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DOCUMENT_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Document already registered"));
    }

    @Test
    void testCreateClient_InvalidEmail_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(clientService.createClient(any(ClientRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Invalid email format",
                        "INVALID_EMAIL",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/clients")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validClientRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_EMAIL"));
    }

    @Test
    void testCreateClient_InvalidPhone_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(clientService.createClient(any(ClientRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Invalid phone format",
                        "INVALID_PHONE",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/clients")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validClientRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PHONE"));
    }

    @Test
    void testCreateClient_MissingName_ShouldReturnBadRequest() throws Exception {
        // Arrange
        ClientRequestDTO invalidRequest = new ClientRequestDTO(
                null,  // Missing name
                "john.doe@example.com",
                "+1234567890",
                "123 Main Street",
                "DNI",
                "12345678"
        );

        when(clientService.createClient(any(ClientRequestDTO.class)))
                .thenThrow(new BusinessException(
                        "Name is required",
                        "INVALID_NAME",
                        400
                ));

        // Act & Assert
        mockMvc.perform(post("/clients")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_NAME"));
    }

    // ==================== GET CLIENT BY ID TESTS ====================

    @Test
    void testGetClientById_ExistingClient_ShouldReturnClient() throws Exception {
        // Arrange
        when(clientService.getClientById(1L)).thenReturn(clientResponse);

        // Act & Assert
        mockMvc.perform(get("/clients/1")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        verify(clientService, times(1)).getClientById(1L);
    }

    @Test
    void testGetClientById_NonExistingClient_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(clientService.getClientById(999L))
                .thenThrow(new BusinessException(
                        "Client not found with id: 999",
                        "CLIENT_NOT_FOUND",
                        404
                ));

        // Act & Assert
        mockMvc.perform(get("/clients/999")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));

        verify(clientService, times(1)).getClientById(999L);
    }

    // ==================== GET CLIENT BY EMAIL TESTS ====================

    @Test
    void testGetClientByEmail_ExistingEmail_ShouldReturnClient() throws Exception {
        // Arrange
        when(clientService.getClientByEmail("john.doe@example.com")).thenReturn(clientResponse);

        // Act & Assert
        mockMvc.perform(get("/clients/email/john.doe@example.com")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.name").value("John Doe"));

        verify(clientService, times(1)).getClientByEmail("john.doe@example.com");
    }

    @Test
    void testGetClientByEmail_NonExistingEmail_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(clientService.getClientByEmail("nonexistent@example.com"))
                .thenThrow(new BusinessException(
                        "Client not found with email: nonexistent@example.com",
                        "CLIENT_NOT_FOUND",
                        404
                ));

        // Act & Assert
        mockMvc.perform(get("/clients/email/nonexistent@example.com")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));
    }

    // ==================== GET ALL CLIENTS TESTS ====================

    @Test
    void testGetAllClients_ShouldReturnClientList() throws Exception {
        // Arrange
        ClientResponseDTO client2 = ClientResponseDTO.builder()
                .id(2L)
                .name("Jane Doe")
                .email("jane.doe@example.com")
                .phone("+0987654321")
                .address("456 Another Street")
                .documentType("PASSPORT")
                .documentNumber("87654321")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<ClientResponseDTO> clients = Arrays.asList(clientResponse, client2);
        when(clientService.getAllClients()).thenReturn(clients);

        // Act & Assert
        mockMvc.perform(get("/clients")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[1].name").value("Jane Doe"));

        verify(clientService, times(1)).getAllClients();
    }

    @Test
    void testGetAllClients_EmptyList_ShouldReturnEmptyArray() throws Exception {
        // Arrange
        when(clientService.getAllClients()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/clients")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==================== UPDATE CLIENT TESTS ====================

    @Test
    void testUpdateClient_ValidData_ShouldReturnUpdatedClient() throws Exception {
        // Arrange
        ClientUpdateDTO updateRequest = new ClientUpdateDTO(
                "John Updated",
                "+9999999999",
                "456 New Address"
        );

        ClientResponseDTO updatedResponse = ClientResponseDTO.builder()
                .id(1L)
                .name("John Updated")
                .email("john.doe@example.com")
                .phone("+9999999999")
                .address("456 New Address")
                .documentType("DNI")
                .documentNumber("12345678")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();

        when(clientService.updateClient(eq(1L), any(ClientUpdateDTO.class))).thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/clients/1")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.phone").value("+9999999999"))
                .andExpect(jsonPath("$.address").value("456 New Address"));

        verify(clientService, times(1)).updateClient(eq(1L), any(ClientUpdateDTO.class));
    }

    @Test
    void testUpdateClient_NonExistingClient_ShouldReturnNotFound() throws Exception {
        // Arrange
        ClientUpdateDTO updateRequest = new ClientUpdateDTO("Updated Name", null, null);

        when(clientService.updateClient(eq(999L), any(ClientUpdateDTO.class)))
                .thenThrow(new BusinessException(
                        "Client not found with id: 999",
                        "CLIENT_NOT_FOUND",
                        404
                ));

        // Act & Assert
        mockMvc.perform(put("/clients/999")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));
    }

    @Test
    void testUpdateClient_DuplicatePhone_ShouldReturnConflict() throws Exception {
        // Arrange
        ClientUpdateDTO updateRequest = new ClientUpdateDTO(null, "+1234567890", null);

        when(clientService.updateClient(eq(1L), any(ClientUpdateDTO.class)))
                .thenThrow(new BusinessException(
                        "Phone already registered",
                        "PHONE_ALREADY_EXISTS",
                        409
                ));

        // Act & Assert
        mockMvc.perform(put("/clients/1")
                        .header("Authorization", "ApiKey test-api-key-12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PHONE_ALREADY_EXISTS"));
    }

    // ==================== DELETE CLIENT TESTS ====================

    @Test
    void testDeleteClient_ExistingClient_ShouldReturnNoContent() throws Exception {
        // Arrange
        doNothing().when(clientService).deleteClient(1L);

        // Act & Assert
        mockMvc.perform(delete("/clients/1")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isNoContent());

        verify(clientService, times(1)).deleteClient(1L);
    }

    @Test
    void testDeleteClient_NonExistingClient_ShouldReturnNotFound() throws Exception {
        // Arrange
        doThrow(new BusinessException(
                "Client not found with id: 999",
                "CLIENT_NOT_FOUND",
                404
        )).when(clientService).deleteClient(999L);

        // Act & Assert
        mockMvc.perform(delete("/clients/999")
                        .header("Authorization", "ApiKey test-api-key-12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CLIENT_NOT_FOUND"));

        verify(clientService, times(1)).deleteClient(999L);
    }

    // ==================== API KEY AUTHENTICATION TESTS ====================

    @Test
    void testCreateClient_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validClientRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verify(clientService, never()).createClient(any());
    }

    @Test
    void testCreateClient_InvalidApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/clients")
                        .header("Authorization", "ApiKey wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validClientRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verify(clientService, never()).createClient(any());
    }

    @Test
    void testGetClientById_MissingApiKey_ShouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/clients/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verify(clientService, never()).getClientById(anyLong());
    }
}
