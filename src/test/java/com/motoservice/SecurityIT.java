package com.motoservice;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

class SecurityIT extends PostgresTestBase {
  @Test
  void unauthenticatedIs401() throws Exception {
    mvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());
  }

  @Test
  void actualBasicPasswordWorks() throws Exception {
    mvc.perform(get("/api/motorcycles").with(httpBasic("c1", "TestRide21!")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].registration").value("SYNTH1"));
  }

  @Test
  void wrongPasswordRejected() throws Exception {
    mvc.perform(get("/api/bookings").with(httpBasic("c1", "wrong")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void customersCannotSeeOtherBookings() throws Exception {
    service.book("c1", input(1));
    mvc.perform(get("/api/bookings").with(user("c2")))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void advisorEndpointForbiddenForCustomer() throws Exception {
    mvc.perform(
            patch("/api/advisor/bookings/1/status")
                .with(user("c1").roles("CUSTOMER"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"IN_SERVICE\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidInputIs400() throws Exception {
    mvc.perform(
            post("/api/bookings")
                .with(user("c1"))
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void browserRequiresCsrf() throws Exception {
    mvc.perform(
            post("/motorcycles")
                .with(user("c1"))
                .param("registration", "DEMO99")
                .param("model", "Roadster"))
        .andExpect(status().isForbidden());
  }

  @Test
  void apiRequiresCsrfToo() throws Exception {
    mvc.perform(
            post("/api/bookings")
                .with(httpBasic("c1", "TestRide21!"))
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void validApiBookingIs201() throws Exception {
    mvc.perform(
            post("/api/bookings")
                .with(user("c1"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"motorcycleId\":1,\"slotId\":1,\"serviceType\":\"REGULAR_SERVICE\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reference").value("MSL-1"));
  }

  @Test
  void crossOwnerApiIs403() throws Exception {
    mvc.perform(
            post("/api/bookings")
                .with(user("c1"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"motorcycleId\":2,\"slotId\":1,\"serviceType\":\"REGULAR_SERVICE\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void missingSlotIs404() throws Exception {
    mvc.perform(get("/api/slots/999").with(user("c1"))).andExpect(status().isNotFound());
  }

  @Test
  void loginAndTemplatesRender() throws Exception {
    mvc.perform(get("/login"))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
    mvc.perform(get("/").with(user("c1"))).andExpect(status().isOk());
    mvc.perform(get("/slots").with(user("c1"))).andExpect(status().isOk());
    mvc.perform(get("/").with(user("advisor").roles("SERVICE_ADVISOR"))).andExpect(status().isOk());
  }

  @Test
  void formLoginWorks() throws Exception {
    mvc.perform(
            post("/login").with(csrf()).param("username", "c1").param("password", "TestRide21!"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }
}
