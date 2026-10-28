package com.motoservice.repository;

import com.motoservice.entity.*;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<ServiceSlot, Long> {
  @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from ServiceSlot s where s.id = :id")
  Optional<ServiceSlot> lockById(@Param("id") Long id);

  List<ServiceSlot> findByStartsAtGreaterThanEqualOrderByStartsAt(Instant from);
}
