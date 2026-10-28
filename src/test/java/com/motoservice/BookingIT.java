package com.motoservice;

import static org.junit.jupiter.api.Assertions.*;

import com.motoservice.entity.*;
import com.motoservice.exception.BusinessException;
import org.junit.jupiter.api.Test;

class BookingIT extends PostgresTestBase {
  @Test
  void successfulBookingAndHistoryPersist() {
    var b = service.book("c1", input(1));
    assertEquals(BookingStatus.CONFIRMED, b.status());
    assertEquals(1, b.history().size());
    assertEquals(1, occupancy());
  }

  @Test
  void fullSlotRejected() {
    capacity(1);
    service.book("c1", input(1));
    assertThrows(BusinessException.class, () -> service.book("c2", input(2)));
    assertEquals(1, occupancy());
  }

  @Test
  void duplicateRejected() {
    service.book("c1", input(1));
    assertThrows(BusinessException.class, () -> service.book("c1", input(1)));
    assertEquals(1, occupancy());
  }

  @Test
  void ownershipRejected() {
    assertThrows(BusinessException.class, () -> service.book("c1", input(2)));
    assertEquals(0, occupancy());
  }

  @Test
  void cancellationPromotesFirst() {
    capacity(1);
    var b = service.book("c1", input(1));
    service.join("c2", input(2));
    service.join("c3", input(3));
    service.cancel("c1", b.id());
    assertEquals(WaitlistStatus.PROMOTED, service.waitlist("c2").getFirst().status());
    assertEquals(WaitlistStatus.WAITING, service.waitlist("c3").getFirst().status());
    assertEquals(1, occupancy());
  }

  @Test
  void tiesUseIdOrder() {
    capacity(1);
    var b = service.book("c1", input(1));
    var first = service.join("c2", input(2));
    service.join("c3", input(3));
    jdbc.update("UPDATE waitlist_entries SET requested_at='2030-01-01T00:00:00Z'");
    service.cancel("c1", b.id());
    assertEquals(first.id(), service.waitlist("c2").getFirst().id());
    assertEquals(WaitlistStatus.PROMOTED, service.waitlist("c2").getFirst().status());
    assertEquals(WaitlistStatus.WAITING, service.waitlist("c3").getFirst().status());
  }

  @Test
  void withdrawnEntrySkipped() {
    capacity(1);
    var b = service.book("c1", input(1));
    var w = service.join("c2", input(2));
    service.join("c3", input(3));
    service.withdraw("c2", w.id());
    service.cancel("c1", b.id());
    assertEquals(WaitlistStatus.PROMOTED, service.waitlist("c3").getFirst().status());
  }

  @Test
  void ineligibleOwnerSkipped() {
    capacity(1);
    var b = service.book("c1", input(1));
    service.join("c2", input(2));
    service.join("c3", input(3));
    jdbc.update("UPDATE motorcycles SET owner_id=4 WHERE id=2");
    service.cancel("c1", b.id());
    assertEquals(WaitlistStatus.INELIGIBLE, service.waitlist("c2").getFirst().status());
    assertEquals(WaitlistStatus.PROMOTED, service.waitlist("c3").getFirst().status());
  }

  @Test
  void waitlistMustBeFullAndUnique() {
    assertThrows(BusinessException.class, () -> service.join("c1", input(1)));
    capacity(1);
    service.book("c1", input(1));
    service.join("c2", input(2));
    assertThrows(BusinessException.class, () -> service.join("c2", input(2)));
    assertThrows(BusinessException.class, () -> service.join("c1", input(1)));
  }

  @Test
  void advisorWorkflow() {
    var b = service.book("c1", input(1));
    assertThrows(
        BusinessException.class, () -> service.status("advisor", b.id(), BookingStatus.COMPLETED));
    service.status("advisor", b.id(), BookingStatus.IN_SERVICE);
    var complete = service.status("advisor", b.id(), BookingStatus.COMPLETED);
    assertEquals(3, complete.history().size());
    assertThrows(BusinessException.class, () -> service.cancel("c1", b.id()));
    assertEquals(1, occupancy());
  }

  @Test
  void pastSlotRejected() {
    jdbc.update("UPDATE service_slots SET starts_at=now()-interval '1 minute'");
    assertThrows(BusinessException.class, () -> service.book("c1", input(1)));
  }

  @Test
  void doubleCancellationRejected() {
    var b = service.book("c1", input(1));
    service.cancel("c1", b.id());
    assertThrows(BusinessException.class, () -> service.cancel("c1", b.id()));
    assertEquals(0, occupancy());
  }

  @Test
  void crossCustomerCancelRejected() {
    var b = service.book("c1", input(1));
    assertThrows(BusinessException.class, () -> service.cancel("c2", b.id()));
    assertEquals(1, occupancy());
  }

  @Test
  void cancellationAndPromotionRollbackTogether() {
    capacity(1);
    var b = service.book("c1", input(1));
    service.join("c2", input(2));
    jdbc.execute(
        "CREATE FUNCTION reject_promotion() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF"
            + " NEW.customer_id=2 THEN RAISE EXCEPTION 'injected failure'; END IF; RETURN NEW; END"
            + " $$");
    jdbc.execute(
        "CREATE TRIGGER fail_promotion BEFORE INSERT ON bookings FOR EACH ROW EXECUTE FUNCTION"
            + " reject_promotion()");
    try {
      assertThrows(RuntimeException.class, () -> service.cancel("c1", b.id()));
      assertEquals(BookingStatus.CONFIRMED, service.bookings("c1").getFirst().status());
      assertEquals(WaitlistStatus.WAITING, service.waitlist("c2").getFirst().status());
      assertEquals(1, count("SELECT count(*) FROM booking_events"));
    } finally {
      jdbc.execute("DROP TRIGGER fail_promotion ON bookings");
      jdbc.execute("DROP FUNCTION reject_promotion()");
    }
  }
}
