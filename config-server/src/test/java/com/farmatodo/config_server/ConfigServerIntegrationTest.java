package com.farmatodo.config_server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class ConfigServerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldReturnConfigurationForApplication() throws Exception {
		// Test fetching configuration for a specific application in default profile
		// Format: /{application}/{profile}
		mockMvc.perform(get("/api-gateway/default"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("api-gateway"))
				.andExpect(jsonPath("$.profiles").isNotEmpty());
	}

	@Test
	void shouldReturnConfigurationForApplicationWithProfile() throws Exception {
		// Test fetching configuration with specific profile
		// Format: /{application}/{profile}
		mockMvc.perform(get("/client-service/dev"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("client-service"));
	}

	@Test
	void shouldReturnConfigurationForApplicationWithLabel() throws Exception {
		// Test fetching configuration with label (branch/tag)
		// Format: /{application}/{profile}/{label}
		mockMvc.perform(get("/token-service/default/main"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("token-service"))
				.andExpect(jsonPath("$.label").exists());
	}

	@Test
	void shouldHandleNonExistentApplication() throws Exception {
		// Config server should still return 200 for non-existent apps
		// but with minimal/default configuration
		mockMvc.perform(get("/non-existent-service/default"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("non-existent-service"));
	}

	@Test
	void shouldReturnHealthStatus() throws Exception {
		// Test health endpoint
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").exists());
	}

	@Test
	void shouldReturnInfo() throws Exception {
		// Test info endpoint
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isOk());
	}

	@Test
	void shouldReturnConfigurationInPropertiesFormat() throws Exception {
		// Test properties format endpoint
		// Format: /{application}-{profile}.properties
		mockMvc.perform(get("/product-service-default.properties"))
				.andExpect(status().isOk());
	}

	@Test
	void shouldReturnConfigurationInYamlFormat() throws Exception {
		// Test YAML format endpoint
		// Format: /{application}-{profile}.yml
		mockMvc.perform(get("/cart-service-default.yml"))
				.andExpect(status().isOk());
	}

	@Test
	void shouldReturnConfigurationInJsonFormat() throws Exception {
		// Test JSON format endpoint
		// Format: /{application}-{profile}.json
		mockMvc.perform(get("/order-service-default.json"))
				.andExpect(status().isOk());
	}

}
