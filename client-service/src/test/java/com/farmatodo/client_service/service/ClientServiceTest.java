package com.farmatodo.client_service.service;

import com.farmatodo.client_service.dto.ClientRequestDTO;
import com.farmatodo.client_service.dto.ClientResponseDTO;
import com.farmatodo.client_service.dto.ClientUpdateDTO;
import com.farmatodo.client_service.exception.BusinessException;
import com.farmatodo.client_service.model.Client;
import com.farmatodo.client_service.repository.ClientRepository;
import com.farmatodo.client_service.util.ClientValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientValidator clientValidator;

    @InjectMocks
    private ClientService clientService;

    private ClientRequestDTO validClientRequest;
    private Client savedClient;

    @BeforeEach
    void setUp() {
        MDC.put("transactionId", "test-transaction-id");

        validClientRequest = new ClientRequestDTO(
                "John Doe",
                "john.doe@example.com",
                "+1234567890",
                "123 Main Street, City",
                "DNI",
                "12345678"
        );

        savedClient = Client.builder()
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

    @Test
    void testCreateClient_HappyPath_ShouldReturnClientResponse() {
        // Arrange
        doNothing().when(clientValidator).validateName(anyString());
        doNothing().when(clientValidator).validateEmail(anyString());
        doNothing().when(clientValidator).validatePhone(anyString());
        doNothing().when(clientValidator).validateAddress(anyString());
        doNothing().when(clientValidator).validateDocumentType(anyString());
        doNothing().when(clientValidator).validateDocumentNumber(anyString());

        when(clientValidator.normalizeEmail(anyString())).thenReturn("john.doe@example.com");
        when(clientValidator.cleanPhone(anyString())).thenReturn("+1234567890");
        when(clientValidator.normalizeDocumentType(anyString())).thenReturn("DNI");

        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.existsByPhone(anyString())).thenReturn(false);
        when(clientRepository.existsByDocumentTypeAndDocumentNumber(anyString(), anyString())).thenReturn(false);

        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);

        // Act
        ClientResponseDTO response = clientService.createClient(validClientRequest);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("John Doe", response.getName());
        assertEquals("john.doe@example.com", response.getEmail());
        assertEquals("+1234567890", response.getPhone());
        assertEquals("ACTIVE", response.getStatus());

        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void testCreateClient_EmailAlreadyExists_ShouldThrowException() {
        // Arrange
        doNothing().when(clientValidator).validateName(anyString());
        doNothing().when(clientValidator).validateEmail(anyString());
        doNothing().when(clientValidator).validatePhone(anyString());
        doNothing().when(clientValidator).validateAddress(anyString());
        doNothing().when(clientValidator).validateDocumentType(anyString());
        doNothing().when(clientValidator).validateDocumentNumber(anyString());

        when(clientValidator.normalizeEmail(anyString())).thenReturn("john.doe@example.com");
        when(clientValidator.cleanPhone(anyString())).thenReturn("+1234567890");
        when(clientValidator.normalizeDocumentType(anyString())).thenReturn("DNI");

        when(clientRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            clientService.createClient(validClientRequest);
        });

        assertEquals("EMAIL_ALREADY_EXISTS", exception.getErrorCode());
        assertEquals(409, exception.getHttpStatus());

        verify(clientRepository, never()).save(any());
    }

    @Test
    void testCreateClient_PhoneAlreadyExists_ShouldThrowException() {
        // Arrange
        doNothing().when(clientValidator).validateName(anyString());
        doNothing().when(clientValidator).validateEmail(anyString());
        doNothing().when(clientValidator).validatePhone(anyString());
        doNothing().when(clientValidator).validateAddress(anyString());
        doNothing().when(clientValidator).validateDocumentType(anyString());
        doNothing().when(clientValidator).validateDocumentNumber(anyString());

        when(clientValidator.normalizeEmail(anyString())).thenReturn("john.doe@example.com");
        when(clientValidator.cleanPhone(anyString())).thenReturn("+1234567890");
        when(clientValidator.normalizeDocumentType(anyString())).thenReturn("DNI");

        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.existsByPhone(anyString())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            clientService.createClient(validClientRequest);
        });

        assertEquals("PHONE_ALREADY_EXISTS", exception.getErrorCode());
        assertEquals(409, exception.getHttpStatus());

        verify(clientRepository, never()).save(any());
    }

    @Test
    void testCreateClient_DocumentAlreadyExists_ShouldThrowException() {
        // Arrange
        doNothing().when(clientValidator).validateName(anyString());
        doNothing().when(clientValidator).validateEmail(anyString());
        doNothing().when(clientValidator).validatePhone(anyString());
        doNothing().when(clientValidator).validateAddress(anyString());
        doNothing().when(clientValidator).validateDocumentType(anyString());
        doNothing().when(clientValidator).validateDocumentNumber(anyString());

        when(clientValidator.normalizeEmail(anyString())).thenReturn("john.doe@example.com");
        when(clientValidator.cleanPhone(anyString())).thenReturn("+1234567890");
        when(clientValidator.normalizeDocumentType(anyString())).thenReturn("DNI");

        when(clientRepository.existsByEmail(anyString())).thenReturn(false);
        when(clientRepository.existsByPhone(anyString())).thenReturn(false);
        when(clientRepository.existsByDocumentTypeAndDocumentNumber(anyString(), anyString())).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            clientService.createClient(validClientRequest);
        });

        assertEquals("DOCUMENT_ALREADY_EXISTS", exception.getErrorCode());
        assertEquals(409, exception.getHttpStatus());

        verify(clientRepository, never()).save(any());
    }

    @Test
    void testGetClientById_Found_ShouldReturnClient() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.of(savedClient));

        // Act
        ClientResponseDTO response = clientService.getClientById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("John Doe", response.getName());
    }

    @Test
    void testGetClientById_NotFound_ShouldThrowException() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            clientService.getClientById(1L);
        });

        assertEquals("CLIENT_NOT_FOUND", exception.getErrorCode());
        assertEquals(404, exception.getHttpStatus());
    }

    @Test
    void testGetClientByEmail_Found_ShouldReturnClient() {
        // Arrange
        when(clientValidator.normalizeEmail(anyString())).thenReturn("john.doe@example.com");
        when(clientRepository.findByEmail(anyString())).thenReturn(Optional.of(savedClient));

        // Act
        ClientResponseDTO response = clientService.getClientByEmail("john.doe@example.com");

        // Assert
        assertNotNull(response);
        assertEquals("john.doe@example.com", response.getEmail());
    }

    @Test
    void testGetAllClients_ShouldReturnList() {
        // Arrange
        List<Client> clients = Arrays.asList(savedClient);
        when(clientRepository.findAll()).thenReturn(clients);

        // Act
        List<ClientResponseDTO> response = clientService.getAllClients();

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("John Doe", response.get(0).getName());
    }

    @Test
    void testUpdateClient_HappyPath_ShouldReturnUpdatedClient() {
        // Arrange
        ClientUpdateDTO updateRequest = new ClientUpdateDTO(
                "Jane Doe",
                "+0987654321",
                "456 New Street"
        );

        doNothing().when(clientValidator).validateName(anyString());
        doNothing().when(clientValidator).validatePhone(anyString());
        doNothing().when(clientValidator).validateAddress(anyString());
        when(clientValidator.cleanPhone(anyString())).thenReturn("+0987654321");

        when(clientRepository.findById(1L)).thenReturn(Optional.of(savedClient));
        when(clientRepository.existsByPhone(anyString())).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);

        // Act
        ClientResponseDTO response = clientService.updateClient(1L, updateRequest);

        // Assert
        assertNotNull(response);
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void testDeleteClient_ShouldSetStatusInactive() {
        // Arrange
        when(clientRepository.findById(1L)).thenReturn(Optional.of(savedClient));
        when(clientRepository.save(any(Client.class))).thenReturn(savedClient);

        // Act
        clientService.deleteClient(1L);

        // Assert
        verify(clientRepository).save(argThat(client ->
                "INACTIVE".equals(client.getStatus())
        ));
    }
}
