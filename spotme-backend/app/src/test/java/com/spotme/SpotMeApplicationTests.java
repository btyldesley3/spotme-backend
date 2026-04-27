package com.spotme;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "grpc.server.port=0",
        "spring.flyway.enabled=true"
})
@ActiveProfiles("test")
@Import(SpotMeApplicationTests.EmbeddedPostgresConfig.class)
class SpotMeApplicationTests {

  private static final UUID SEEDED_ALLOWLIST_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  private static final UUID SEEDED_INVITE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
  private static final String SEEDED_ALLOWLIST_EMAIL = "seeded-alpha@spotme.dev";
  private static final String SEEDED_INVITE_CODE = "SEEDED-ALPHA-CODE";

  private static final EmbeddedPostgres postgres = startEmbeddedPostgres();

  @Autowired
  private JdbcTemplate jdbc;

  @BeforeEach
  void cleanupSeedRows() {
    jdbc.update("delete from alpha_email_allowlist where id = ?", SEEDED_ALLOWLIST_ID);
    jdbc.update("delete from alpha_invite_codes where id = ?", SEEDED_INVITE_ID);
  }

	@Test
	void contextLoads() {
	}

  @Test
  @DisplayName("Flyway creates auth tables on clean bootstrap")
  void flywayCreatesAuthTables() {
    List<String> authTables = jdbc.queryForList(
            """
                    select table_name
                    from information_schema.tables
                    where table_schema = 'public'
                      and table_name in ('alpha_invite_codes', 'alpha_email_allowlist', 'user_credentials', 'refresh_tokens')
                    order by table_name
                    """,
            String.class
    );

    assertEquals(List.of("alpha_email_allowlist", "alpha_invite_codes", "refresh_tokens", "user_credentials"), authTables);
    Integer flywayRows = jdbc.queryForObject("select count(*) from flyway_schema_history where success = true", Integer.class);
    assertTrue(flywayRows != null && flywayRows >= 3, "Expected at least V1-V3 Flyway migrations to be applied");
  }

  @Test
  @DisplayName("Test seed flow is deterministic via idempotent upserts")
  void authSeedFlowIsDeterministic() {
    upsertAuthSeedRows();
    upsertAuthSeedRows();

    Integer allowlistCount = jdbc.queryForObject(
            "select count(*) from alpha_email_allowlist where email = ?",
            Integer.class,
            SEEDED_ALLOWLIST_EMAIL
    );
    Integer inviteCount = jdbc.queryForObject(
            "select count(*) from alpha_invite_codes where id = ?",
            Integer.class,
            SEEDED_INVITE_ID
    );
    Integer usedCount = jdbc.queryForObject(
            "select used_count from alpha_invite_codes where id = ?",
            Integer.class,
            SEEDED_INVITE_ID
    );

    assertEquals(1, allowlistCount);
    assertEquals(1, inviteCount);
    assertEquals(0, usedCount);
  }

  private void upsertAuthSeedRows() {
    jdbc.update(
            "insert into alpha_email_allowlist (id, email, active, notes, created_at) values (?, ?, true, ?, now()) " +
                    "on conflict (email) do update set active = excluded.active, notes = excluded.notes",
            SEEDED_ALLOWLIST_ID,
            SEEDED_ALLOWLIST_EMAIL,
            "deterministic-seed"
    );

    jdbc.update(
            "insert into alpha_invite_codes (id, code_hash, active, max_uses, used_count, expires_at, created_at) " +
                    "values (?, ?, true, 5, 0, ?, now()) " +
                    "on conflict (id) do update set active = excluded.active, max_uses = excluded.max_uses, " +
                    "used_count = excluded.used_count, expires_at = excluded.expires_at",
            SEEDED_INVITE_ID,
            sha256(SEEDED_INVITE_CODE),
            Timestamp.from(Instant.now().plusSeconds(3600))
    );
  }

  private static String sha256(String rawValue) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      var bytes = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
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
