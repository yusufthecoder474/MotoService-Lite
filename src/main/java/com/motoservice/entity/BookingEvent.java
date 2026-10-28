package com.motoservice.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "booking_events")
public class BookingEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long bookingId;

  @Enumerated(EnumType.STRING)
  private BookingStatus status;

  private Instant occurredAt;
  private String detail;

  protected BookingEvent() {}

  public BookingEvent(Long bookingId, BookingStatus status, Instant occurredAt, String detail) {
    this.bookingId = bookingId;
    this.status = status;
    this.occurredAt = occurredAt;
    this.detail = detail;
  }

  public Long getId() {
    return id;
  }

  public Long getBookingId() {
    return bookingId;
  }

  public BookingStatus getStatus() {
    return status;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getDetail() {
    return detail;
  }
}
