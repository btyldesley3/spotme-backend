package com.spotme;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;

@SpringBootTest(properties = {
        "grpc.server.port=0",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
@Import(SpotMeApplicationTests.EmbeddedPostgresConfig.class)
class SpotMeApplicationTests {

  private static final EmbeddedPostgres postgres = startEmbeddedPostgres();

	@Test
	void contextLoads() {
	}

  private static EmbeddedPostgres startEmbeddedPostgres() {
    try {
      return EmbeddedPostgres.builder().start();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to start embedded Postgres for application context test", e);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class EmbeddedPostgresConfig {

    @Bean
    DataSource dataSource() {
      return postgres.getPostgresDatabase();
    }
  }

}
