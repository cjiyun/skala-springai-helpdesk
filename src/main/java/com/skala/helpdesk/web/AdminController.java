package com.skala.helpdesk.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skala.helpdesk.service.TicketService;
import com.skala.helpdesk.dto.TicketView;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/tickets")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "HelpDesk · 교환·환불 승인")
public class AdminController {
  private final TicketService tickets;

  public AdminController(TicketService tickets) {
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
