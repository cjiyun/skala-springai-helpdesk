package com.skala.helpdesk.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skala.helpdesk.domain.Order;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, String> {

  Optional<Order> findByIdAndOwnerId(String orderId, String userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Order o where o.id = :orderId and o.ownerId = :userId")
  Optional<Order> findOwnedByIdForUpdate(@Param("orderId") String orderId, @Param("userId") String userId);
}
