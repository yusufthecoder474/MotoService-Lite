package com.motoservice.repository;

import com.motoservice.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface MotorcycleRepository extends JpaRepository<Motorcycle, Long> {
  List<Motorcycle> findByOwnerIdOrderById(Long ownerId);
}
