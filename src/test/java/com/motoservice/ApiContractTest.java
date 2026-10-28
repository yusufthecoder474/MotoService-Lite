package com.motoservice;

import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.motoservice.config.SecurityConfig;
import com.motoservice.controller.ApiController;
import com.motoservice.exception.BusinessException;
import com.motoservice.repository.UserRepository;
import com.motoservice.service.WorkshopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ApiController.class)
@Import(SecurityConfig.class)
class ApiContractTest {
  @Autowired MockMvc mvc;
  @MockitoBean WorkshopService service;
  @MockitoBean UserRepository users;

  @Test
  void badBasicCredentialsReturn401Json() throws Exception {
    mvc.perform(get("/api/bookings").with(httpBasic("unknown", "wrong")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void anonymousReadReturns401Json() throws Exception {
    mvc.perform(get("/api/bookings"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void customerCannotUseAdvisorRoute() throws Exception {
    mvc.perform(
            patch("/api/advisor/bookings/1/status")
                .with(user("c1").roles("CUSTOMER"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"status\":\"IN_SERVICE\"}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void writeRequiresCsrf() throws Exception {
    mvc.perform(
            post("/api/bookings").with(user("c1")).contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void invalidRequestIs400() throws Exception {
    mvc.perform(
            post("/api/bookings")
                .with(user("c1"))
                .with(csrf())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
    verifyNoInteractions(service);
  }

  @Test
  void missingResourceIs404() throws Exception {
    when(service.slot(999L)).thenThrow(new BusinessException(NOT_FOUND, "Resource not found"));
    mvc.perform(get("/api/slots/999").with(user("c1")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.path").value("/api/slots/999"));
  }

  @Test
  void businessConflictIs409() throws Exception {
    when(service.book(eq("c1"), any()))
        .thenThrow(new BusinessException(CONFLICT, "Slot is full; join the waitlist"));
    mvc.perform(
            post("/api/bookings")
                .with(user("c1"))
                .with(csrf())
                .contentType("application/json")
                .content("{\"motorcycleId\":1,\"slotId\":1,\"serviceType\":\"REGULAR_SERVICE\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }
}
