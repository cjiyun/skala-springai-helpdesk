package com.skala.lab0.myapp.lab3.ticket;

public class TicketAlreadyApprovedException extends RuntimeException {
  public TicketAlreadyApprovedException() {
    super("이미 승인된 티켓입니다.");
  }
}
