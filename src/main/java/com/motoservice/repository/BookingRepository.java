package com.motoservice.repository;

import com.motoservice.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {
  long countBySlotIdAndStatusNot(Long slotId, BookingStatus status);

  boolean existsByMotorcycleIdAndSlotIdAndStatusNot(
      Long motorcycleId, Long slotId, BookingStatus status);

  List<Booking> findByCustomerIdOrderByIdDesc(Long customerId);

  List<Booking> findAllByOrderByIdDesc();

  @Query("select b.slotId from Booking b where b.id=:id")
  Optional<Long> slotId(@Param("id") Long id);
}
