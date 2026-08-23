package com.skala.helpdesk.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skala.helpdesk.domain.Ticket;
import com.skala.helpdesk.domain.TicketStatus;
import com.skala.helpdesk.domain.TicketType;

public interface TicketRepository extends JpaRepository<Ticket, String> {
  List<Ticket> findByStatusOrderByCreatedAtAsc(TicketStatus status);

  Optional<Ticket> findFirstByOrderIdAndUserIdAndTypeAndStatus(
      String orderId, String userId, TicketType type, TicketStatus status);

  Optional<Ticket> findFirstByOrderIdAndUserIdOrderByCreatedAtDesc(String orderId, String userId);

  long countByOrderIdAndUserIdAndTypeAndStatus(
      String orderId, String userId, TicketType type, TicketStatus status);
}
