package com.motoservice;

import com.motoservice.dto.Requests.BookingInput;
import com.motoservice.entity.*;
import com.motoservice.service.WorkshopService;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(properties = {"app.demo=false", "spring.datasource.hikari.maximum-pool-size=60"})
@AutoConfigureMockMvc
abstract class PostgresTestBase {
  private static PostgreSQLContainer<?> container;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    String external = System.getenv("TEST_DB_URL");
    if (external != null) {
      if (!external.matches("jdbc:postgresql://[^/]+/motoservice_test"))
        throw new IllegalArgumentException(
            "Tests require a dedicated motoservice_test database, without URL query parameters");
      registry.add("spring.datasource.url", () -> external);
      registry.add(
          "spring.datasource.username",
          () -> System.getenv().getOrDefault("TEST_DB_USER", "motoservice"));
      registry.add(
          "spring.datasource.password",
          () -> System.getenv().getOrDefault("TEST_DB_PASSWORD", "local-demo-only"));
    } else {
      container = new PostgreSQLContainer<>("postgres:16").withDatabaseName("motoservice_test");
      container.start();
      registry.add("spring.datasource.url", container::getJdbcUrl);
      registry.add("spring.datasource.username", container::getUsername);
      registry.add("spring.datasource.password", container::getPassword);
    }
  }

  @Autowired WorkshopService service;
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @Autowired PasswordEncoder passwords;

  @BeforeEach
  void resetSyntheticData() {
    jdbc.execute(
        "TRUNCATE"
            + " booking_events,waitlist_entries,bookings,motorcycles,service_slots,service_centers,users"
            + " RESTART IDENTITY CASCADE");
    String hash = passwords.encode("TestRide21!");
    for (int i = 1; i <= 60; i++) {
      jdbc.update(
          "INSERT INTO users(username,password_hash,role) VALUES (?,?,'CUSTOMER')", "c" + i, hash);
      jdbc.update(
          "INSERT INTO motorcycles(owner_id,registration,model) VALUES (?,?,?)",
          i,
          "SYNTH" + i,
          "Fictional Roadster");
    }
    jdbc.update(
        "INSERT INTO users(username,password_hash,role) VALUES ('advisor',?,'SERVICE_ADVISOR')",
        hash);
    jdbc.update("INSERT INTO service_centers(name) VALUES ('Synthetic workshop')");
    jdbc.update(
        "INSERT INTO service_slots(center_id,starts_at,capacity) VALUES (1,?,2)",
        Timestamp.from(Instant.now().plusSeconds(86400)));
  }

  BookingInput input(int customer) {
    return new BookingInput((long) customer, 1L, ServiceType.REGULAR_SERVICE, "Synthetic request");
  }

  long occupancy() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM bookings WHERE slot_id=1 AND status<>'CANCELLED'", Long.class);
  }

  void capacity(int n) {
    jdbc.update("UPDATE service_slots SET capacity=? WHERE id=1", n);
  }

  long count(String sql) {
    return jdbc.queryForObject(sql, Long.class);
  }
}
