package com.motoservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "motorcycles")
public class Motorcycle {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long ownerId;
  private String registration;
  private String model;
  private String note;

  protected Motorcycle() {}

  public Motorcycle(Long ownerId, String registration, String model, String note) {
    this.ownerId = ownerId;
    this.registration = registration;
    this.model = model;
    this.note = note;
  }

  public Long getId() {
    return id;
  }

  public Long getOwnerId() {
    return ownerId;
  }

  public String getRegistration() {
    return registration;
  }

  public String getModel() {
    return model;
  }

  public String getNote() {
    return note;
  }
}
