package com.motoservice.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "service_slots")
public class ServiceSlot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long centerId;
  private Instant startsAt;
  private int capacity;

  protected ServiceSlot() {}

  public ServiceSlot(Long centerId, Instant startsAt, int capacity) {
    this.centerId = centerId;
    this.startsAt = startsAt;
    this.capacity = capacity;
  }

  public Long getId() {
    return id;
  }

  public Long getCenterId() {
    return centerId;
  }

  public Instant getStartsAt() {
    return startsAt;
  }

  public int getCapacity() {
    return capacity;
  }
}
