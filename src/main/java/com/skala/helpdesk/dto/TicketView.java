package com.skala.helpdesk.dto;

import com.skala.helpdesk.domain.Ticket;

public record TicketView(
    String ticketNo,
    String orderId,
    String type,
    String status,
    String message) {
  public static TicketView from(Ticket ticket, String message) {
    return new TicketView(
        ticket.getId(),
        ticket.getOrderId(),
        ticket.getType().name(),
        ticket.getStatus().name(),
        message);
  }
}
