package com.motoservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "service_centers")
public class ServiceCenter {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  protected ServiceCenter() {}

  public ServiceCenter(String name) {
    this.name = name;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
