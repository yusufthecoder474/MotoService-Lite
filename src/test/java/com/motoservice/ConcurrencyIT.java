package com.motoservice;

import static org.junit.jupiter.api.Assertions.*;

import com.motoservice.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ConcurrencyIT extends PostgresTestBase {
  @ParameterizedTest
  @CsvSource({"20,2", "50,5"})
  void capacitySafe(int n, int cap) throws Exception {
    capacity(cap);
    var result =
        ConcurrentRunner.run(
            n,
            i -> {
              try {
                service.book("c" + (i + 1), input(i + 1));
                return true;
              } catch (BusinessException e) {
                assertEquals(409, e.status().value());
                return false;
              }
            });
    assertEquals(cap, result.stream().filter(Boolean::booleanValue).count());
    assertEquals(cap, occupancy());
  }

  @Test
  void duplicateSubmissions() throws Exception {
    var result =
        ConcurrentRunner.run(
            20,
            i -> {
              try {
                service.book("c1", input(1));
                return true;
              } catch (BusinessException e) {
                assertEquals(409, e.status().value());
                return false;
              }
            });
    assertEquals(1, result.stream().filter(Boolean::booleanValue).count());
    assertEquals(1, occupancy());
  }

  @Test
  void simultaneousCancellationsPromoteExactlyTwo() throws Exception {
    var a = service.book("c1", input(1));
    var b = service.book("c2", input(2));
    for (int i = 3; i <= 12; i++) service.join("c" + i, input(i));
    ConcurrentRunner.run(2, i -> service.cancel("c" + (i + 1), i == 0 ? a.id() : b.id()));
    assertEquals(2, occupancy());
    assertEquals(2, count("SELECT count(*) FROM waitlist_entries WHERE status='PROMOTED'"));
    assertEquals(
        2,
        count("SELECT count(DISTINCT booking_id) FROM waitlist_entries WHERE status='PROMOTED'"));
    assertEquals(
        2,
        count("SELECT count(*) FROM bookings WHERE customer_id IN (3,4) AND status='CONFIRMED'"));
  }

  @Test
  void sameBookingCannotCancelTwiceConcurrently() throws Exception {
    capacity(1);
    var a = service.book("c1", input(1));
    service.join("c2", input(2));
    service.join("c3", input(3));
    var result =
        ConcurrentRunner.run(
            2,
            i -> {
              try {
                service.cancel("c1", a.id());
                return true;
              } catch (BusinessException e) {
                return false;
              }
            });
    assertEquals(1, result.stream().filter(Boolean::booleanValue).count());
    assertEquals(1, occupancy());
    assertEquals(1, count("SELECT count(*) FROM waitlist_entries WHERE status='PROMOTED'"));
  }
}
