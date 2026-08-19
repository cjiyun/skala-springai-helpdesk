package com.skala.lab0.myapp.order.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skala.lab0.myapp.order.domain.Order;

public interface OrderRepository extends JpaRepository<Order, String> {

  Optional<Order> findByIdAndOwnerId(String orderId, String userId);
}
