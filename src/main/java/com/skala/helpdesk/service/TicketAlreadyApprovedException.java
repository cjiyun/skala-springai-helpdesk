package com.skala.helpdesk.service;

public class TicketAlreadyApprovedException extends RuntimeException {
  public TicketAlreadyApprovedException() {
    super("이미 승인된 티켓입니다.");
  }
}
