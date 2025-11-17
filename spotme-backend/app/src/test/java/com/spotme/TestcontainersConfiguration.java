package com.spotme;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");


//	@Bean
//	@ServiceConnection(name = "redis")
//	GenericContainer<?> redisContainer() {
//		return new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);
//	}
//

}
