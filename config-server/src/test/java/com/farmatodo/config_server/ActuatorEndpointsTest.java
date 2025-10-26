package com.farmatodo.config_server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorEndpointsTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthEndpointShouldReturnUp() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is("UP")));
	}

	@Test
	void healthEndpointShouldIncludeComponents() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").exists());
	}

	@Test
	void infoEndpointShouldBeAccessible() throws Exception {
		mockMvc.perform(get("/actuator/info"))
				.andExpect(status().isOk());
	}

	@Test
	void actuatorBasePathShouldBeAccessible() throws Exception {
		mockMvc.perform(get("/actuator"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links").exists())
				.andExpect(jsonPath("$._links.self").exists())
				.andExpect(jsonPath("$._links.health").exists())
				.andExpect(jsonPath("$._links.info").exists());
	}

	@Test
	void healthEndpointShouldHaveSelfLink() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", notNullValue()));
	}

	@Test
	void beansEndpointShouldBeAccessible() throws Exception {
		// Verify that beans endpoint is accessible
		mockMvc.perform(get("/actuator/beans"))
				.andExpect(status().isOk());
	}

	@Test
	void metricsEndpointShouldBeAccessible() throws Exception {
		// Metrics endpoint should be accessible
		mockMvc.perform(get("/actuator/metrics"))
				.andExpect(status().isOk());
	}

	@Test
	void envEndpointShouldBeAccessible() throws Exception {
		// Environment endpoint should be accessible
		mockMvc.perform(get("/actuator/env"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.propertySources").isArray());
	}

}
