package com.farmatodo.cart_service;

import com.farmatodo.cart_service.client.ProductServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.cloud.config.enabled=false",
		"spring.cloud.config.import-check.enabled=false"
})
class CartServiceApplicationTests {

	@MockitoBean
	private ProductServiceClient productServiceClient;

	@Test
	void contextLoads() {
	}

}
