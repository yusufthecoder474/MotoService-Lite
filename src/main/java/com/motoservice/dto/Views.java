package com.motoservice.dto;

import com.motoservice.entity.*;
import java.time.Instant;
import java.util.List;

public final class Views {
  private Views() {}

  public record CenterView(Long id, String name) {}

  public record MotorcycleView(Long id, String registration, String model, String note) {}

  public record SlotView(
      Long id,
      Long centerId,
      String center,
      Instant startsAt,
      int capacity,
      long available,
      boolean full) {}

  public record EventView(BookingStatus status, Instant occurredAt, String detail) {}

  public record BookingView(
      Long id,
      String reference,
      Long motorcycleId,
      Long slotId,
      ServiceType serviceType,
      String note,
      BookingStatus status,
      String registration,
      String center,
      Instant startsAt,
      boolean cancellable,
      List<EventView> history) {}

  public record WaitlistView(
      Long id,
      Long motorcycleId,
      Long slotId,
      WaitlistStatus status,
      Instant requestedAt,
      Long bookingId,
      long position) {}
}
