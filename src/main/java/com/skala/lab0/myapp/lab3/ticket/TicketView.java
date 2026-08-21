package com.skala.lab0.myapp.lab3.ticket;

public record TicketView(
    String ticketNo,
    String orderId,
    String type,
    String status,
    String message) {
  static TicketView from(Ticket ticket, String message) {
    return new TicketView(
        ticket.getId(),
        ticket.getOrderId(),
        ticket.getType().name(),
        ticket.getStatus().name(),
        message);
  }
}
