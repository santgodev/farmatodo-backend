package com.farmatodo.order_service;

import com.farmatodo.order_service.client.ClientServiceClient;
import com.farmatodo.order_service.client.TokenServiceClient;
import com.farmatodo.order_service.client.ProductServiceClient;
import com.farmatodo.order_service.client.CartServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.cloud.config.enabled=false",
		"spring.cloud.config.import-check.enabled=false",
		"spring.mail.username=test@farmatodo.com"
})
class OrderServiceApplicationTests {

	@MockitoBean
	private ClientServiceClient clientServiceClient;

	@MockitoBean
	private TokenServiceClient tokenServiceClient;

	@MockitoBean
	private ProductServiceClient productServiceClient;

	@MockitoBean
	private CartServiceClient cartServiceClient;

	@MockitoBean
	private JavaMailSender javaMailSender;

	@Test
	void contextLoads() {
	}

}
