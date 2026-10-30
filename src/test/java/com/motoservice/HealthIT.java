package com.motoservice;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContext;

class HealthIT extends PostgresTestBase {
  @Autowired ApplicationContext context;

  @Test
  void anonymousProbesRevealOnlyStatus() throws Exception {
    for (String probe : new String[] {"liveness", "readiness"})
      mvc.perform(get("/actuator/health/" + probe))
          .andExpect(status().isOk())
          .andExpect(content().json("{\"status\":\"UP\"}", true));
  }

  @Test
  void refusingTrafficDoesNotFailLiveness() throws Exception {
    try {
      AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
      mvc.perform(get("/actuator/health/readiness")).andExpect(status().isServiceUnavailable());
      mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    } finally {
      AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
    }
  }

  @Test
  void otherActuatorPathsDeniedToAdvisor() throws Exception {
    for (String path :
        new String[] {"/actuator", "/actuator/health", "/actuator/env", "/actuator/metrics"})
      mvc.perform(get(path).with(user("advisor").roles("SERVICE_ADVISOR")))
          .andExpect(status().isForbidden());
  }

  @Test
  void probePostDoesNotBypassCsrf() throws Exception {
    mvc.perform(post("/actuator/health/readiness")).andExpect(status().isForbidden());
    mvc.perform(post("/api/bookings").with(user("c1").roles("CUSTOMER")))
        .andExpect(status().isForbidden());
  }
}
