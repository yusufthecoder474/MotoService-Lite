package com.motoservice.tools;

import java.util.Arrays;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Offline terminal helper: never accepts credentials as command-line arguments. */
public final class PasswordHash {
  public static void main(String[] args) {
    var console = System.console();
    if (console == null || args.length != 0)
      throw new IllegalStateException("Use an interactive terminal, without arguments");
    char[] password = console.readPassword("New staging password (16+ characters): ");
    if (password == null) throw new IllegalStateException("Cancelled");
    try {
      if (password.length < 16) throw new IllegalArgumentException("Use at least 16 characters");
      console.printf("%s%n", new BCryptPasswordEncoder().encode(new String(password)));
    } finally {
      Arrays.fill(password, '\0');
    }
  }
}
