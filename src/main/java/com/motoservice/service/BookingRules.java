package com.motoservice.service;

import static org.springframework.http.HttpStatus.CONFLICT;

import com.motoservice.entity.BookingStatus;
import com.motoservice.exception.BusinessException;
import java.time.Instant;

public final class BookingRules {
  private BookingRules() {}

  public static void future(Instant start, Instant now) {
    if (!start.isAfter(now)) throw new BusinessException(CONFLICT, "Slot has already started");
  }

  public static void cancellable(BookingStatus status, Instant start, Instant now) {
    future(start, now);
    if (status != BookingStatus.CONFIRMED)
      throw new BusinessException(CONFLICT, "Only future confirmed bookings can be cancelled");
  }

  public static void transition(BookingStatus from, BookingStatus to) {
    if (!((from == BookingStatus.CONFIRMED && to == BookingStatus.IN_SERVICE)
        || (from == BookingStatus.IN_SERVICE && to == BookingStatus.COMPLETED)))
      throw new BusinessException(CONFLICT, "Allowed workflow: CONFIRMED → IN_SERVICE → COMPLETED");
  }
}
