package com.skala.lab0.myapp.lab3.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketPersistenceTest {
  @Autowired
  TicketService service;

  @Autowired
  TicketRepository tickets;

  @Autowired
  EntityManager entityManager;

  @Autowired
  MockMvc mvc;

  @Test
  void 환불_접수부터_관리자_승인까지_DB에_상태가_유지된다() throws Exception {
    TicketView requested = service.requestRefund("12345", "user1", "단순 변심");
    entityManager.flush();
    entityManager.clear();

    Ticket pending = tickets.findById(requested.ticketNo()).orElseThrow();
    assertThat(pending.getStatus()).isEqualTo(TicketStatus.PENDING);
    assertThat(pending.getApprovedAt()).isNull();

    mvc.perform(get("/api/admin/tickets/pending")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.ticketNo == '%s')]".formatted(requested.ticketNo())).exists());

    mvc.perform(post("/api/admin/tickets/{ticketNo}/approve", requested.ticketNo())
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
    entityManager.flush();
    entityManager.clear();

    Ticket approved = tickets.findById(requested.ticketNo()).orElseThrow();
    assertThat(approved.getStatus()).isEqualTo(TicketStatus.APPROVED);
    assertThat(approved.getApprovedAt()).isNotNull();
  }
}
