package com.skala.lab0.myapp.lab3.ticket;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, String> {
  List<Ticket> findByStatusOrderByCreatedAtAsc(TicketStatus status);

  Optional<Ticket> findFirstByOrderIdAndUserIdAndTypeAndStatus(
      String orderId, String userId, TicketType type, TicketStatus status);

  Optional<Ticket> findFirstByOrderIdAndUserIdOrderByCreatedAtDesc(String orderId, String userId);

  long countByOrderIdAndUserIdAndTypeAndStatus(
      String orderId, String userId, TicketType type, TicketStatus status);
}
