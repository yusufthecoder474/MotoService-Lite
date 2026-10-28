package com.motoservice.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bookings")
public class Booking {
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
  private BookingStatus status;

  private Instant createdAt;

  protected Booking() {}

  public Booking(
      Long customerId,
      Long motorcycleId,
      Long slotId,
      ServiceType serviceType,
      String note,
      BookingStatus status,
      Instant createdAt) {
    this.customerId = customerId;
    this.motorcycleId = motorcycleId;
    this.slotId = slotId;
    this.serviceType = serviceType;
    this.note = note;
    this.status = status;
    this.createdAt = createdAt;
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

  public BookingStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void changeStatus(BookingStatus status) {
    this.status = status;
  }
}
