package com.motoservice.service;

import static org.springframework.http.HttpStatus.*;

import com.motoservice.dto.Requests.*;
import com.motoservice.dto.Views.*;
import com.motoservice.entity.*;
import com.motoservice.exception.BusinessException;
import com.motoservice.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkshopService {
  private final UserRepository users;
  private final MotorcycleRepository bikes;
  private final CenterRepository centers;
  private final SlotRepository slots;
  private final BookingRepository bookings;
  private final WaitlistRepository queue;
  private final EventRepository events;
  private final Clock clock;

  public WorkshopService(
      UserRepository users,
      MotorcycleRepository bikes,
      CenterRepository centers,
      SlotRepository slots,
      BookingRepository bookings,
      WaitlistRepository queue,
      EventRepository events,
      Clock clock) {
    this.users = users;
    this.bikes = bikes;
    this.centers = centers;
    this.slots = slots;
    this.bookings = bookings;
    this.queue = queue;
    this.events = events;
    this.clock = clock;
  }

  private <T> T found(Optional<T> value) {
    return value.orElseThrow(() -> new BusinessException(NOT_FOUND, "Resource not found"));
  }

  private AppUser user(String name) {
    return found(users.findByUsername(name));
  }

  private void customer(AppUser user) {
    if (user.getRole() != Role.CUSTOMER)
      throw new BusinessException(FORBIDDEN, "Customer role required");
  }

  private Motorcycle owned(Long id, Long owner) {
    Motorcycle m = found(bikes.findById(id));
    if (!m.getOwnerId().equals(owner))
      throw new BusinessException(FORBIDDEN, "Motorcycle belongs to another customer");
    return m;
  }

  private ServiceSlot lock(Long id) {
    return found(slots.lockById(id));
  }

  private long occupied(Long slot) {
    return bookings.countBySlotIdAndStatusNot(slot, BookingStatus.CANCELLED);
  }

  private void duplicate(Long bike, Long slot) {
    if (bookings.existsByMotorcycleIdAndSlotIdAndStatusNot(bike, slot, BookingStatus.CANCELLED))
      throw new BusinessException(CONFLICT, "Motorcycle already has a booking for this slot");
  }

  public boolean isAdvisor(String name) {
    return user(name).getRole() == Role.SERVICE_ADVISOR;
  }

  public List<CenterView> centers() {
    return centers.findAll().stream().map(c -> new CenterView(c.getId(), c.getName())).toList();
  }

  public List<MotorcycleView> motorcycles(String name) {
    return bikes.findByOwnerIdOrderById(user(name).getId()).stream().map(this::bikeView).toList();
  }

  private MotorcycleView bikeView(Motorcycle m) {
    return new MotorcycleView(m.getId(), m.getRegistration(), m.getModel(), m.getNote());
  }

  @Transactional
  public MotorcycleView register(String name, MotorcycleInput input) {
    AppUser u = user(name);
    customer(u);
    return bikeView(
        bikes.saveAndFlush(
            new Motorcycle(
                u.getId(),
                input.registration().replace(" ", "").toUpperCase(Locale.ROOT),
                input.model(),
                input.note())));
  }

  public List<SlotView> slots(LocalDate date) {
    Instant from =
        date == null ? clock.instant() : date.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
    Instant until =
        date == null
            ? Instant.MAX
            : date.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant();
    return slots.findByStartsAtGreaterThanEqualOrderByStartsAt(from).stream()
        .filter(s -> s.getStartsAt().isBefore(until))
        .map(this::slotView)
        .toList();
  }

  public SlotView slot(Long id) {
    return slotView(found(slots.findById(id)));
  }

  private SlotView slotView(ServiceSlot s) {
    long available = s.getCapacity() - occupied(s.getId());
    return new SlotView(
        s.getId(),
        s.getCenterId(),
        found(centers.findById(s.getCenterId())).getName(),
        s.getStartsAt(),
        s.getCapacity(),
        available,
        available == 0);
  }

  public List<BookingView> bookings(String name) {
    AppUser u = user(name);
    return (u.getRole() == Role.SERVICE_ADVISOR
            ? bookings.findAllByOrderByIdDesc()
            : bookings.findByCustomerIdOrderByIdDesc(u.getId()))
        .stream().map(this::bookingView).toList();
  }

  private BookingView bookingView(Booking b) {
    ServiceSlot slot = found(slots.findById(b.getSlotId()));
    return new BookingView(
        b.getId(),
        "MSL-" + b.getId(),
        b.getMotorcycleId(),
        b.getSlotId(),
        b.getServiceType(),
        b.getNote(),
        b.getStatus(),
        found(bikes.findById(b.getMotorcycleId())).getRegistration(),
        found(centers.findById(slot.getCenterId())).getName(),
        slot.getStartsAt(),
        b.getStatus() == BookingStatus.CONFIRMED && slot.getStartsAt().isAfter(clock.instant()),
        events.findByBookingIdOrderById(b.getId()).stream()
            .map(e -> new EventView(e.getStatus(), e.getOccurredAt(), e.getDetail()))
            .toList());
  }

  public List<WaitlistView> waitlist(String name) {
    AppUser u = user(name);
    return (u.getRole() == Role.SERVICE_ADVISOR
            ? queue.findAllByOrderByRequestedAtAscIdAsc()
            : queue.findByCustomerIdOrderByRequestedAtAscIdAsc(u.getId()))
        .stream().map(this::waitView).toList();
  }

  private WaitlistView waitView(WaitlistEntry w) {
    return new WaitlistView(
        w.getId(),
        w.getMotorcycleId(),
        w.getSlotId(),
        w.getStatus(),
        w.getRequestedAt(),
        w.getBookingId(),
        w.getStatus() == WaitlistStatus.WAITING
            ? queue.position(w.getSlotId(), w.getRequestedAt(), w.getId())
            : 0);
  }

  private Booking create(
      Long owner, Long bike, Long slot, ServiceType type, String note, String detail) {
    Booking b =
        bookings.saveAndFlush(
            new Booking(owner, bike, slot, type, note, BookingStatus.CONFIRMED, clock.instant()));
    event(b, detail);
    return b;
  }

  private void event(Booking b, String detail) {
    events.save(new BookingEvent(b.getId(), b.getStatus(), clock.instant(), detail));
  }

  @Transactional
  public BookingView book(String name, BookingInput input) {
    AppUser u = user(name);
    customer(u);
    ServiceSlot s = lock(input.slotId()); // Every mutation acquires this same lock FIRST.
    BookingRules.future(s.getStartsAt(), clock.instant());
    owned(input.motorcycleId(), u.getId());
    duplicate(input.motorcycleId(), s.getId());
    if (queue.existsByMotorcycleIdAndSlotIdAndStatus(
        input.motorcycleId(), s.getId(), WaitlistStatus.WAITING))
      throw new BusinessException(CONFLICT, "Already waitlisted; withdraw before booking");
    if (occupied(s.getId()) >= s.getCapacity())
      throw new BusinessException(CONFLICT, "Slot is full; join the waitlist");
    return bookingView(
        create(
            u.getId(),
            input.motorcycleId(),
            s.getId(),
            input.serviceType(),
            input.note(),
            "Booking confirmed"));
  }

  @Transactional
  public WaitlistView join(String name, BookingInput input) {
    AppUser u = user(name);
    customer(u);
    ServiceSlot s = lock(input.slotId());
    BookingRules.future(s.getStartsAt(), clock.instant());
    owned(input.motorcycleId(), u.getId());
    duplicate(input.motorcycleId(), s.getId());
    if (occupied(s.getId()) < s.getCapacity())
      throw new BusinessException(CONFLICT, "Space is available; book directly");
    if (queue.existsByMotorcycleIdAndSlotIdAndStatus(
        input.motorcycleId(), s.getId(), WaitlistStatus.WAITING))
      throw new BusinessException(CONFLICT, "Already on this waitlist");
    return waitView(
        queue.saveAndFlush(
            new WaitlistEntry(
                u.getId(),
                input.motorcycleId(),
                s.getId(),
                input.serviceType(),
                input.note(),
                WaitlistStatus.WAITING,
                clock.instant(),
                null)));
  }

  @Transactional
  public BookingView cancel(String name, Long id) {
    AppUser u = user(name);
    customer(u);
    // Scalar projection avoids caching a stale Booking before acquiring the slot lock.
    ServiceSlot s = lock(found(bookings.slotId(id)));
    Booking b = found(bookings.findById(id));
    if (!b.getCustomerId().equals(u.getId()))
      throw new BusinessException(FORBIDDEN, "Booking belongs to another customer");
    BookingRules.cancellable(b.getStatus(), s.getStartsAt(), clock.instant());
    b.changeStatus(BookingStatus.CANCELLED);
    bookings.flush();
    event(b, "Cancelled by customer");
    promote(s);
    return bookingView(b);
  }

  private void promote(ServiceSlot s) {
    while (occupied(s.getId()) < s.getCapacity()) {
      Optional<WaitlistEntry> next =
          queue.findFirstBySlotIdAndStatusOrderByRequestedAtAscIdAsc(
              s.getId(), WaitlistStatus.WAITING);
      if (next.isEmpty()) return;
      WaitlistEntry w = next.get();
      boolean eligible =
          s.getStartsAt().isAfter(clock.instant())
              && bikes
                  .findById(w.getMotorcycleId())
                  .map(m -> m.getOwnerId().equals(w.getCustomerId()))
                  .orElse(false)
              && users
                  .findById(w.getCustomerId())
                  .map(u -> u.getRole() == Role.CUSTOMER)
                  .orElse(false)
              && !bookings.existsByMotorcycleIdAndSlotIdAndStatusNot(
                  w.getMotorcycleId(), s.getId(), BookingStatus.CANCELLED);
      if (!eligible) {
        w.close(WaitlistStatus.INELIGIBLE, null);
        queue.flush();
        continue;
      }
      Booking b =
          create(
              w.getCustomerId(),
              w.getMotorcycleId(),
              s.getId(),
              w.getServiceType(),
              w.getNote(),
              "Promoted from FIFO waitlist #" + w.getId());
      w.close(WaitlistStatus.PROMOTED, b.getId());
      queue.flush();
    }
  }

  @Transactional
  public WaitlistView withdraw(String name, Long id) {
    AppUser u = user(name);
    customer(u);
    lock(found(queue.slotId(id)));
    WaitlistEntry w = found(queue.findById(id));
    if (!w.getCustomerId().equals(u.getId()))
      throw new BusinessException(FORBIDDEN, "Waitlist belongs to another customer");
    if (w.getStatus() != WaitlistStatus.WAITING)
      throw new BusinessException(CONFLICT, "Only waiting entries can be withdrawn");
    w.close(WaitlistStatus.WITHDRAWN, null);
    return waitView(w);
  }

  @Transactional
  public BookingView status(String name, Long id, BookingStatus status) {
    if (!isAdvisor(name)) throw new BusinessException(FORBIDDEN, "Service advisor required");
    lock(found(bookings.slotId(id)));
    Booking b = found(bookings.findById(id));
    BookingRules.transition(b.getStatus(), status);
    b.changeStatus(status);
    event(b, "Updated by service advisor");
    return bookingView(b);
  }
}
