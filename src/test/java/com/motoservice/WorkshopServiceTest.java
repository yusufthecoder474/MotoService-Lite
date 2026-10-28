package com.motoservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.motoservice.dto.Requests.BookingInput;
import com.motoservice.entity.*;
import com.motoservice.exception.BusinessException;
import com.motoservice.repository.*;
import com.motoservice.service.WorkshopService;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkshopServiceTest {
  @Mock UserRepository users;
  @Mock MotorcycleRepository bikes;
  @Mock CenterRepository centers;
  @Mock SlotRepository slots;
  @Mock BookingRepository bookings;
  @Mock WaitlistRepository queue;
  @Mock EventRepository events;
  WorkshopService service;

  @BeforeEach
  void setup() {
    service =
        new WorkshopService(
            users,
            bikes,
            centers,
            slots,
            bookings,
            queue,
            events,
            Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void advisorCannotBook() {
    when(users.findByUsername("advisor"))
        .thenReturn(Optional.of(new AppUser("advisor", "hash", Role.SERVICE_ADVISOR)));
    assertThrows(
        BusinessException.class,
        () -> service.book("advisor", new BookingInput(1L, 1L, ServiceType.REGULAR_SERVICE, null)));
    verifyNoInteractions(slots, bookings);
  }

  @Test
  void customerCannotUpdateStatus() {
    when(users.findByUsername("c"))
        .thenReturn(Optional.of(new AppUser("c", "hash", Role.CUSTOMER)));
    assertThrows(BusinessException.class, () -> service.status("c", 1L, BookingStatus.IN_SERVICE));
    verifyNoInteractions(slots, bookings);
  }

  @Test
  void duplicateRejectedUnderSlotLock() {
    AppUser user = mock(AppUser.class);
    when(user.getId()).thenReturn(1L);
    when(user.getRole()).thenReturn(Role.CUSTOMER);
    when(users.findByUsername("c")).thenReturn(Optional.of(user));
    ServiceSlot slot = mock(ServiceSlot.class);
    when(slot.getId()).thenReturn(2L);
    when(slot.getStartsAt()).thenReturn(Instant.parse("2030-02-01T00:00:00Z"));
    when(slots.lockById(2L)).thenReturn(Optional.of(slot));
    when(bikes.findById(3L)).thenReturn(Optional.of(new Motorcycle(1L, "DEMO3", "Roadster", null)));
    when(bookings.existsByMotorcycleIdAndSlotIdAndStatusNot(3L, 2L, BookingStatus.CANCELLED))
        .thenReturn(true);
    assertThrows(
        BusinessException.class,
        () -> service.book("c", new BookingInput(3L, 2L, ServiceType.REGULAR_SERVICE, null)));
    InOrder order = inOrder(slots, bookings);
    order.verify(slots).lockById(2L);
    order
        .verify(bookings)
        .existsByMotorcycleIdAndSlotIdAndStatusNot(3L, 2L, BookingStatus.CANCELLED);
    verify(bookings, never()).saveAndFlush(any());
  }
}
