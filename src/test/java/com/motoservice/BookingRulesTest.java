package com.motoservice;

import static org.junit.jupiter.api.Assertions.*;

import com.motoservice.entity.BookingStatus;
import com.motoservice.exception.BusinessException;
import com.motoservice.service.BookingRules;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BookingRulesTest {
  private final Instant now = Instant.parse("2030-01-01T10:00:00Z");

  @Test
  void confirmedCanStart() {
    assertDoesNotThrow(
        () -> BookingRules.transition(BookingStatus.CONFIRMED, BookingStatus.IN_SERVICE));
  }

  @Test
  void inServiceCanComplete() {
    assertDoesNotThrow(
        () -> BookingRules.transition(BookingStatus.IN_SERVICE, BookingStatus.COMPLETED));
  }

  @ParameterizedTest
  @EnumSource(BookingStatus.class)
  void terminalStatesCannotTransition(BookingStatus target) {
    assertThrows(
        BusinessException.class, () -> BookingRules.transition(BookingStatus.CANCELLED, target));
    assertThrows(
        BusinessException.class, () -> BookingRules.transition(BookingStatus.COMPLETED, target));
  }

  @Test
  void cannotSkipService() {
    assertThrows(
        BusinessException.class,
        () -> BookingRules.transition(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));
  }

  @Test
  void futureConfirmedCanCancel() {
    assertDoesNotThrow(
        () -> BookingRules.cancellable(BookingStatus.CONFIRMED, now.plusSeconds(1), now));
  }

  @Test
  void startedSlotCannotCancel() {
    assertThrows(
        BusinessException.class, () -> BookingRules.cancellable(BookingStatus.CONFIRMED, now, now));
  }

  @ParameterizedTest
  @EnumSource(
      value = BookingStatus.class,
      names = {"IN_SERVICE", "COMPLETED", "CANCELLED"})
  void otherStatesCannotCancel(BookingStatus status) {
    assertThrows(
        BusinessException.class, () -> BookingRules.cancellable(status, now.plusSeconds(60), now));
  }
}
