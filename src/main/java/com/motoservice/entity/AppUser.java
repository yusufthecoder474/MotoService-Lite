package com.motoservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class AppUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String username;
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  private Role role;

  protected AppUser() {}

  public AppUser(String username, String passwordHash, Role role) {
    this.username = username;
    this.passwordHash = passwordHash;
    this.role = role;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public Role getRole() {
    return role;
  }
}
