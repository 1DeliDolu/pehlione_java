package com.pehlione.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	static {
		configureJnaTempDirectory();
	}

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:8.0.36"));
	}

	private static void configureJnaTempDirectory() {
		String configured = System.getProperty("jna.tmpdir");
		if (configured != null && !configured.isBlank()) {
			return;
		}
		Path jnaTempDirectory = Path.of("target", "jna-tmp").toAbsolutePath().normalize();
		try {
			Files.createDirectories(jnaTempDirectory);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to create JNA temp directory: " + jnaTempDirectory, ex);
		}
		System.setProperty("jna.tmpdir", jnaTempDirectory.toString());
	}

}
