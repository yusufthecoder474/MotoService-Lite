package com.motoservice.dto;

import com.motoservice.entity.*;
import jakarta.validation.constraints.*;

public final class Requests {
  private Requests() {}

  public record MotorcycleInput(
      @NotBlank @Pattern(regexp = "[A-Za-z0-9 -]{4,20}") String registration,
      @NotBlank @Size(max = 80) String model,
      @Size(max = 500) String note) {}

  public record BookingInput(
      @NotNull @Positive Long motorcycleId,
      @NotNull @Positive Long slotId,
      @NotNull ServiceType serviceType,
      @Size(max = 500) String note) {}

  public record StatusInput(@NotNull BookingStatus status) {}
}
