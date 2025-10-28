package com.farmatodo.config_server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.config.server.environment.EnvironmentController;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConfigServerApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		assertThat(applicationContext).isNotNull();
	}

	@Test
	void configServerIsEnabled() {
		// Verify that the EnvironmentController bean exists (indicates config server is enabled)
		assertThat(applicationContext.containsBean("environmentController")).isTrue();
	}

	@Test
	void environmentControllerBeanExists() {
		// Verify that EnvironmentController is properly configured
		EnvironmentController controller = applicationContext.getBean(EnvironmentController.class);
		assertThat(controller).isNotNull();
	}

	@Test
	void applicationNameIsConfigured() {
		String appName = applicationContext.getEnvironment().getProperty("spring.application.name");
		assertThat(appName).isEqualTo("config-server");
	}

	@Test
	void serverPortIsConfigured() {
		String port = applicationContext.getEnvironment().getProperty("server.port");
		assertThat(port).isEqualTo("8888");
	}

	@Test
	void gitUriIsConfigured() {
		String gitUri = applicationContext.getEnvironment().getProperty("spring.cloud.config.server.git.uri");
		assertThat(gitUri).isNotNull();
		assertThat(gitUri).contains("github.com");
	}

	@Test
	void gitDefaultLabelIsConfigured() {
		String defaultLabel = applicationContext.getEnvironment().getProperty("spring.cloud.config.server.git.default-label");
		assertThat(defaultLabel).isEqualTo("master");
	}

	@Test
	void gitCloneOnStartIsEnabled() {
		String cloneOnStart = applicationContext.getEnvironment().getProperty("spring.cloud.config.server.git.clone-on-start");
		assertThat(cloneOnStart).isEqualTo("true");
	}

}
