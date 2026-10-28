package com.motoservice;

import static org.junit.jupiter.api.Assertions.*;

import com.motoservice.dto.Requests.*;
import com.motoservice.entity.ServiceType;
import jakarta.validation.*;
import org.junit.jupiter.api.*;

class ValidationTest {
  static ValidatorFactory factory;
  static Validator validator;

  @BeforeAll
  static void setup() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void close() {
    factory.close();
  }

  @Test
  void acceptsValidBooking() {
    assertTrue(
        validator
            .validate(new BookingInput(1L, 2L, ServiceType.REGULAR_SERVICE, "Check brakes"))
            .isEmpty());
  }

  @Test
  void rejectsMissingBookingFields() {
    assertEquals(3, validator.validate(new BookingInput(null, null, null, null)).size());
  }

  @Test
  void rejectsNonpositiveIds() {
    assertEquals(
        2, validator.validate(new BookingInput(0L, -1L, ServiceType.RUNNING_REPAIR, null)).size());
  }

  @Test
  void rejectsLongNote() {
    assertEquals(
        1,
        validator
            .validate(new BookingInput(1L, 2L, ServiceType.REGULAR_SERVICE, "a".repeat(501)))
            .size());
  }

  @Test
  void validatesMotorcycle() {
    assertTrue(validator.validate(new MotorcycleInput("DEMO100", "Roadster", null)).isEmpty());
    assertFalse(validator.validate(new MotorcycleInput("!", "", null)).isEmpty());
  }
}
