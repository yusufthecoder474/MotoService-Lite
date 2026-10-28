package com.motoservice.config;

import com.motoservice.entity.*;
import com.motoservice.repository.*;
import java.time.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.demo", havingValue = "true")
public class DemoData implements CommandLineRunner {
  private final UserRepository users;
  private final CenterRepository centers;
  private final SlotRepository slots;
  private final MotorcycleRepository bikes;
  private final PasswordEncoder encoder;
  private final Clock clock;

  public DemoData(
      UserRepository users,
      CenterRepository centers,
      SlotRepository slots,
      MotorcycleRepository bikes,
      PasswordEncoder encoder,
      Clock clock) {
    this.users = users;
    this.centers = centers;
    this.slots = slots;
    this.bikes = bikes;
    this.encoder = encoder;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (users.count() > 0) return; // Demo setup only; never rewrites existing records.
    for (String name : new String[] {"customer1", "customer2", "customer3", "customer4"}) {
      AppUser u = users.save(new AppUser(name, encoder.encode("DemoRide21!"), Role.CUSTOMER));
      bikes.save(
          new Motorcycle(
              u.getId(),
              "DEMO" + name.substring(8),
              "Fictional Roadster",
              "Synthetic demo motorcycle"));
    }
    users.save(new AppUser("advisor", encoder.encode("DemoAdvisor21!"), Role.SERVICE_ADVISOR));
    for (String name : new String[] {"Riverside Workshop", "Hillview Workshop"}) {
      ServiceCenter c = centers.save(new ServiceCenter(name));
      for (int d = 1; d <= 14; d++)
        for (int hour : new int[] {9, 11, 14}) {
          Instant start =
              LocalDate.now(clock.withZone(ZoneId.of("Asia/Kolkata")))
                  .plusDays(d)
                  .atTime(hour, 0)
                  .atZone(ZoneId.of("Asia/Kolkata"))
                  .toInstant();
          slots.save(new ServiceSlot(c.getId(), start, 2));
        }
    }
  }
}
