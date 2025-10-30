package com.farmatodo.product_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.cloud.config.enabled=false",
		"spring.cloud.config.import-check.enabled=false"
})
class ProductServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
