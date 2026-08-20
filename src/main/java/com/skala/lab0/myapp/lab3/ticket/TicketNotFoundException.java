package com.skala.lab0.myapp.lab3.ticket;

public class TicketNotFoundException extends RuntimeException {
  public TicketNotFoundException(String ticketNo) {
    super("티켓을 찾을 수 없습니다: " + ticketNo);
  }
}
