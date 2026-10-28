package com.motoservice.controller;

import com.motoservice.dto.Requests.*;
import com.motoservice.dto.Views.*;
import com.motoservice.service.WorkshopService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
  private final WorkshopService service;

  public ApiController(WorkshopService service) {
    this.service = service;
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
  }

  @GetMapping("/centers")
  public List<CenterView> centers() {
    return service.centers();
  }

  @GetMapping("/slots")
  public List<SlotView> slots(@RequestParam(required = false) LocalDate date) {
    return service.slots(date);
  }

  @GetMapping("/slots/{id}")
  public SlotView slot(@PathVariable Long id) {
    return service.slot(id);
  }

  @GetMapping("/motorcycles")
  public List<MotorcycleView> bikes(Principal p) {
    return service.motorcycles(p.getName());
  }

  @PostMapping("/motorcycles")
  @ResponseStatus(HttpStatus.CREATED)
  public MotorcycleView bike(Principal p, @Valid @RequestBody MotorcycleInput input) {
    return service.register(p.getName(), input);
  }

  @GetMapping("/bookings")
  public List<BookingView> bookings(Principal p) {
    return service.bookings(p.getName());
  }

  @PostMapping("/bookings")
  @ResponseStatus(HttpStatus.CREATED)
  public BookingView book(Principal p, @Valid @RequestBody BookingInput input) {
    return service.book(p.getName(), input);
  }

  @PostMapping("/bookings/{id}/cancel")
  public BookingView cancel(Principal p, @PathVariable Long id) {
    return service.cancel(p.getName(), id);
  }

  @GetMapping("/waitlist")
  public List<WaitlistView> waitlist(Principal p) {
    return service.waitlist(p.getName());
  }

  @PostMapping("/waitlist")
  @ResponseStatus(HttpStatus.CREATED)
  public WaitlistView join(Principal p, @Valid @RequestBody BookingInput input) {
    return service.join(p.getName(), input);
  }

  @PostMapping("/waitlist/{id}/withdraw")
  public WaitlistView withdraw(Principal p, @PathVariable Long id) {
    return service.withdraw(p.getName(), id);
  }

  @PatchMapping("/advisor/bookings/{id}/status")
  public BookingView status(
      Principal p, @PathVariable Long id, @Valid @RequestBody StatusInput input) {
    return service.status(p.getName(), id, input.status());
  }
}
