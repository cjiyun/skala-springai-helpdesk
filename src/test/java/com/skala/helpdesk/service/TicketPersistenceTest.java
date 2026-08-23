package com.skala.helpdesk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.skala.helpdesk.domain.Ticket;
import com.skala.helpdesk.domain.TicketStatus;
import com.skala.helpdesk.domain.TicketType;
import com.skala.helpdesk.dto.TicketView;
import com.skala.helpdesk.repository.TicketRepository;

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

  @Test
  void 동일한_PENDING_요청은_DB에_한_건만_저장한다() {
    TicketView first = service.requestExchange("12346", "user1", "색상 변경");
    TicketView repeated = service.requestExchange("12346", "user1", "다시 요청");

    assertThat(repeated.ticketNo()).isEqualTo(first.ticketNo());
    assertThat(tickets.countByOrderIdAndUserIdAndTypeAndStatus(
        "12346", "user1", TicketType.EXCHANGE, TicketStatus.PENDING)).isEqualTo(1);
  }

  @Test
  void 환불과_교환은_별도이며_승인된_티켓은_새_요청을_막지_않는다() {
    TicketView refund = service.requestRefund("12347", "user1", "환불 요청");
    TicketView exchange = service.requestExchange("12347", "user1", "교환 요청");
    service.approve(refund.ticketNo());
    TicketView nextRefund = service.requestRefund("12347", "user1", "승인 후 새 요청");

    assertThat(exchange.ticketNo()).isNotEqualTo(refund.ticketNo());
    assertThat(nextRefund.ticketNo()).isNotEqualTo(refund.ticketNo());
    assertThat(nextRefund.status()).isEqualTo("PENDING");
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void 동시에_같은_요청을_보내도_DB에는_한_건만_저장한다() throws Exception {
    var executor = Executors.newFixedThreadPool(2);
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    try {
      var first = executor.submit(() -> requestTogether(ready, start));
      var second = executor.submit(() -> requestTogether(ready, start));
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      TicketView firstResult = first.get(10, TimeUnit.SECONDS);
      TicketView secondResult = second.get(10, TimeUnit.SECONDS);
      assertThat(secondResult.ticketNo()).isEqualTo(firstResult.ticketNo());
      assertThat(tickets.countByOrderIdAndUserIdAndTypeAndStatus(
          "12346", "user1", TicketType.REFUND, TicketStatus.PENDING)).isEqualTo(1);
      tickets.deleteById(firstResult.ticketNo());
    } finally {
      executor.shutdownNow();
    }
  }

  private TicketView requestTogether(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
    ready.countDown();
    start.await();
    return service.requestRefund("12346", "user1", "동시 요청");
  }
}
