package com.motoservice;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.motoservice.config.SecurityConfig;
import com.motoservice.controller.WebController;
import com.motoservice.dto.Views.*;
import com.motoservice.entity.*;
import com.motoservice.repository.UserRepository;
import com.motoservice.service.WorkshopService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WebController.class)
@Import(SecurityConfig.class)
class WebRenderingTest {
  @Autowired MockMvc mvc;
  @MockitoBean WorkshopService service;
  @MockitoBean UserRepository users;

  private void fixture(String username) {
    Instant time = Instant.parse("2030-01-01T04:00:00Z");
    when(service.motorcycles(username))
        .thenReturn(List.of(new MotorcycleView(1L, "DEMO1", "Roadster", null)));
    when(service.bookings(username))
        .thenReturn(
            List.of(
                new BookingView(
                    1L,
                    "MSL-1",
                    1L,
                    1L,
                    ServiceType.REGULAR_SERVICE,
                    null,
                    BookingStatus.CONFIRMED,
                    "DEMO1",
                    "Workshop",
                    time,
                    true,
                    List.of(new EventView(BookingStatus.CONFIRMED, time, "Booking confirmed")))));
    when(service.waitlist(username))
        .thenReturn(List.of(new WaitlistView(1L, 1L, 1L, WaitlistStatus.WAITING, time, null, 1)));
  }

  @Test
  void loginRendersCsrf() throws Exception {
    mvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("_csrf")));
  }

  @Test
  void customerDashboardRendersRealTemplates() throws Exception {
    fixture("c1");
    mvc.perform(get("/").with(user("c1")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Current FIFO position: 1")))
        .andExpect(content().string(containsString("MSL-1")));
  }

  @Test
  void slotsRenderFormAndTimeZone() throws Exception {
    when(service.slots(null))
        .thenReturn(
            List.of(
                new SlotView(
                    1L, 1L, "Workshop", Instant.parse("2030-01-01T04:00:00Z"), 2, 1, false)));
    when(service.motorcycles("c1"))
        .thenReturn(List.of(new MotorcycleView(1L, "DEMO1", "Roadster", null)));
    mvc.perform(get("/slots").with(user("c1")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("09:30")))
        .andExpect(content().string(containsString("Confirm booking")));
  }

  @Test
  void advisorDashboardRenders() throws Exception {
    fixture("advisor");
    when(service.isAdvisor("advisor")).thenReturn(true);
    when(service.slots(null)).thenReturn(List.of());
    mvc.perform(get("/").with(user("advisor").roles("SERVICE_ADVISOR")))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Start service")));
  }
}
