package com.motoservice.controller;

import com.motoservice.dto.Requests.*;
import com.motoservice.entity.*;
import com.motoservice.exception.BusinessException;
import com.motoservice.service.WorkshopService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {
  private final WorkshopService service;

  public WebController(WorkshopService service) {
    this.service = service;
  }

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @GetMapping("/")
  public String dashboard(Principal p, Model m) {
    boolean advisor = service.isAdvisor(p.getName());
    m.addAttribute("advisor", advisor);
    m.addAttribute("username", p.getName());
    m.addAttribute("bookings", service.bookings(p.getName()));
    m.addAttribute("waitlist", service.waitlist(p.getName()));
    m.addAttribute("bikes", service.motorcycles(p.getName()));
    if (advisor) m.addAttribute("slots", service.slots(null));
    return "dashboard";
  }

  @GetMapping("/motorcycles/new")
  public String motorcycle() {
    return "motorcycle";
  }

  @PostMapping("/motorcycles")
  public String register(
      Principal p,
      @Valid @ModelAttribute MotorcycleInput input,
      BindingResult errors,
      RedirectAttributes flash) {
    if (errors.hasErrors()) {
      flash.addFlashAttribute(
          "error", "Enter a 4–20 character registration, model (up to 80), and note (up to 500).");
      return "redirect:/motorcycles/new";
    }
    service.register(p.getName(), input);
    flash.addFlashAttribute("success", "Motorcycle registered.");
    return "redirect:/";
  }

  @GetMapping("/slots")
  public String slots(Principal p, @RequestParam(required = false) LocalDate date, Model m) {
    m.addAttribute("slots", service.slots(date));
    m.addAttribute("bikes", service.motorcycles(p.getName()));
    m.addAttribute("date", date);
    return "slots";
  }

  @PostMapping("/reserve")
  public String reserve(
      Principal p,
      @Valid @ModelAttribute BookingInput input,
      BindingResult errors,
      @RequestParam String action,
      RedirectAttributes flash) {
    if (errors.hasErrors()) {
      flash.addFlashAttribute(
          "error",
          "Select a motorcycle, slot and service type. Notes must be at most 500 characters.");
      return "redirect:/slots";
    }
    if ("waitlist".equals(action)) {
      var w = service.join(p.getName(), input);
      flash.addFlashAttribute(
          "success",
          "Waitlist request #"
              + w.id()
              + " saved. The earliest eligible request is promoted when space opens.");
    } else {
      var b = service.book(p.getName(), input);
      flash.addFlashAttribute("success", "Confirmed: " + b.reference());
    }
    return "redirect:/";
  }

  @PostMapping("/bookings/{id}/cancel")
  public String cancel(Principal p, @PathVariable Long id, RedirectAttributes flash) {
    service.cancel(p.getName(), id);
    flash.addFlashAttribute(
        "success", "Booking cancelled; eligible waitlist promotion was processed.");
    return "redirect:/";
  }

  @PostMapping("/waitlist/{id}/withdraw")
  public String withdraw(Principal p, @PathVariable Long id, RedirectAttributes flash) {
    service.withdraw(p.getName(), id);
    flash.addFlashAttribute("success", "Waitlist request withdrawn.");
    return "redirect:/";
  }

  @PostMapping("/advisor/bookings/{id}/status")
  public String status(
      Principal p,
      @PathVariable Long id,
      @RequestParam BookingStatus status,
      RedirectAttributes flash) {
    service.status(p.getName(), id, status);
    flash.addFlashAttribute("success", "Service status updated.");
    return "redirect:/";
  }

  @ExceptionHandler({
    BusinessException.class,
    org.springframework.dao.DataIntegrityViolationException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  public String error(Exception ex, RedirectAttributes flash) {
    flash.addFlashAttribute(
        "error",
        ex instanceof BusinessException
            ? ex.getMessage()
            : "Invalid or duplicate record. Check your input and retry.");
    return "redirect:/";
  }
}
