package com.motoservice.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long customerId;
  private Long motorcycleId;
  private Long slotId;

  @Enumerated(EnumType.STRING)
  private ServiceType serviceType;

  private String note;

  @Enumerated(EnumType.STRING)
  private WaitlistStatus status;

  private Instant requestedAt;
  private Long bookingId;

  protected WaitlistEntry() {}

  public WaitlistEntry(
      Long customerId,
      Long motorcycleId,
      Long slotId,
      ServiceType serviceType,
      String note,
      WaitlistStatus status,
      Instant requestedAt,
      Long bookingId) {
    this.customerId = customerId;
    this.motorcycleId = motorcycleId;
    this.slotId = slotId;
    this.serviceType = serviceType;
    this.note = note;
    this.status = status;
    this.requestedAt = requestedAt;
    this.bookingId = bookingId;
  }

  public Long getId() {
    return id;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public Long getMotorcycleId() {
    return motorcycleId;
  }

  public Long getSlotId() {
    return slotId;
  }

  public ServiceType getServiceType() {
    return serviceType;
  }

  public String getNote() {
    return note;
  }

  public WaitlistStatus getStatus() {
    return status;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public Long getBookingId() {
    return bookingId;
  }

  public void close(WaitlistStatus status, Long bookingId) {
    this.status = status;
    this.bookingId = bookingId;
  }
}
