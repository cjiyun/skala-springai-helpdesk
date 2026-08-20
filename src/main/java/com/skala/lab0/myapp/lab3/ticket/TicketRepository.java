package com.skala.lab0.myapp.lab3.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, String> {
  List<Ticket> findByStatusOrderByCreatedAtAsc(TicketStatus status);
}
