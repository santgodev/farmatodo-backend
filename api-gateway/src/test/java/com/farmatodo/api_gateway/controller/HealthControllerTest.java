package com.farmatodo.api_gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false"
})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void ping_ShouldReturnPong() throws Exception {
        mockMvc.perform(get("/api/gateway/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    void health_ShouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/api/gateway/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("api-gateway"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void info_ShouldReturnServiceInfo() throws Exception {
        mockMvc.perform(get("/api/gateway/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("api-gateway"))
                .andExpect(jsonPath("$.version").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.endpoints").isArray());
    }
}
