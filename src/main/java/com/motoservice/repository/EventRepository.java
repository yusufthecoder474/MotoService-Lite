package com.motoservice.repository;

import com.motoservice.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EventRepository extends JpaRepository<BookingEvent, Long> {
  List<BookingEvent> findByBookingIdOrderById(Long bookingId);
}
