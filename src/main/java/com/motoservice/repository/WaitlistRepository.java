package com.motoservice.repository;

import com.motoservice.entity.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {
  Optional<WaitlistEntry> findFirstBySlotIdAndStatusOrderByRequestedAtAscIdAsc(
      Long slotId, WaitlistStatus status);

  boolean existsByMotorcycleIdAndSlotIdAndStatus(
      Long motorcycleId, Long slotId, WaitlistStatus status);

  List<WaitlistEntry> findByCustomerIdOrderByRequestedAtAscIdAsc(Long customerId);

  List<WaitlistEntry> findAllByOrderByRequestedAtAscIdAsc();

  @Query(
      "select count(w) from WaitlistEntry w where w.slotId=:slot and"
          + " w.status=com.motoservice.entity.WaitlistStatus.WAITING and (w.requestedAt<:time or"
          + " (w.requestedAt=:time and w.id<=:id))")
  long position(@Param("slot") Long slot, @Param("time") Instant time, @Param("id") Long id);

  @Query("select w.slotId from WaitlistEntry w where w.id=:id")
  Optional<Long> slotId(@Param("id") Long id);
}
