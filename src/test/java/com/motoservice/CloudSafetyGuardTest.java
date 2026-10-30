package com.motoservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.motoservice.config.CloudSafetyGuard;
import com.motoservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class CloudSafetyGuardTest {
  private MockEnvironment env() {
    return new MockEnvironment()
        .withProperty(
            "spring.datasource.url",
            "jdbc:postgresql://db.example/motoservice?sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory");
  }

  @Test
  void acceptsVerifiedTlsWithoutDemoUsers() {
    assertDoesNotThrow(() -> new CloudSafetyGuard(env(), mock(UserRepository.class)).run(null));
  }

  @Test
  void rejectsDemoFlag() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new CloudSafetyGuard(env().withProperty("app.demo", "true"), mock(UserRepository.class))
                .run(null));
  }

  @Test
  void rejectsWeakTls() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new CloudSafetyGuard(
                    env()
                        .withProperty(
                            "spring.datasource.url",
                            "jdbc:postgresql://db.example/motoservice?sslmode=require"),
                    mock(UserRepository.class))
                .run(null));
  }

  @Test
  void rejectsPersistedDemoAccount() {
    var users = mock(UserRepository.class);
    when(users.findByUsername("customer1"))
        .thenReturn(java.util.Optional.of(mock(com.motoservice.entity.AppUser.class)));
    assertThrows(IllegalStateException.class, () -> new CloudSafetyGuard(env(), users).run(null));
  }

  @Test
  void rejectsDuplicateTlsParameters() {
    var e = env();
    e.withProperty(
        "spring.datasource.url", e.getProperty("spring.datasource.url") + "&sslmode=disable");
    assertThrows(
        IllegalStateException.class,
        () -> new CloudSafetyGuard(e, mock(UserRepository.class)).run(null));
  }
}
