package com.skala.lab0.myapp.lab3.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skala.lab0.myapp.lab3.ticket.TicketService;
import com.skala.lab0.myapp.lab3.ticket.TicketView;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/lab3/admin/tickets")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Day3 실습 · 환불 승인")
public class Lab3AdminController {
  private final TicketService tickets;

  public Lab3AdminController(TicketService tickets) {
    this.tickets = tickets;
  }

  @GetMapping("/pending")
  public List<TicketView> pending() {
    return tickets.pending();
  }

  @PostMapping("/{ticketNo}/approve")
  public TicketView approve(@PathVariable String ticketNo) {
    return tickets.approve(ticketNo);
  }
}
