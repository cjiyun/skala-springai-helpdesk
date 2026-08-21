package com.skala.lab0.myapp.lab3.ticket;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skala.lab0.myapp.order.repository.OrderRepository;
import com.skala.lab0.myapp.order.service.OrderNotFoundException;

@Service
public class TicketService {
  private final OrderRepository orders;
  private final TicketRepository tickets;

  public TicketService(OrderRepository orders, TicketRepository tickets) {
    this.orders = orders;
    this.tickets = tickets;
  }

  @Transactional
  public TicketView requestRefund(String orderId, String userId, String reason) {
    return request(orderId, userId, TicketType.REFUND, reason);
  }

  @Transactional
  public TicketView requestExchange(String orderId, String userId, String reason) {
    return request(orderId, userId, TicketType.EXCHANGE, reason);
  }

  private TicketView request(String orderId, String userId, TicketType type, String reason) {
    String normalizedOrderId = required(orderId, "orderId");
    String normalizedUserId = required(userId, "userId");
    String normalizedReason = required(reason, "reason");
    orders.findByIdAndOwnerId(normalizedOrderId, normalizedUserId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));

    Ticket ticket = tickets.save(new Ticket(
        UUID.randomUUID().toString(),
        normalizedOrderId,
        normalizedUserId, type,
        normalizedReason,
        Instant.now()));
    return TicketView.from(ticket, type == TicketType.REFUND
        ? "환불이 접수되었습니다. 담당자 승인 후 처리됩니다."
        : "교환이 접수되었습니다. 담당자 승인 후 처리됩니다.");
  }

  @Transactional(readOnly = true)
  public List<TicketView> pending() {
    return tickets.findByStatusOrderByCreatedAtAsc(TicketStatus.PENDING).stream()
        .map(ticket -> TicketView.from(ticket, "승인 대기 중입니다."))
        .toList();
  }

  @Transactional
  public TicketView approve(String ticketNo) {
    Ticket ticket = tickets.findById(ticketNo)
        .orElseThrow(() -> new TicketNotFoundException(ticketNo));
    ticket.approve(Instant.now());
    return TicketView.from(ticket, (ticket.getType() == TicketType.REFUND ? "환불" : "교환") + " 티켓이 승인되었습니다.");
  }

  private String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.trim();
  }
}
