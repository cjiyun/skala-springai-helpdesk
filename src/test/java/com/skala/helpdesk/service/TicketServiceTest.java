package com.skala.helpdesk.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.skala.helpdesk.domain.Order;
import com.skala.helpdesk.domain.OrderStatus;
import com.skala.helpdesk.domain.Ticket;
import com.skala.helpdesk.domain.TicketStatus;
import com.skala.helpdesk.domain.TicketType;
import com.skala.helpdesk.dto.TicketView;
import com.skala.helpdesk.repository.OrderRepository;
import com.skala.helpdesk.repository.TicketRepository;

class TicketServiceTest {
  private OrderRepository orders;
  private TicketRepository tickets;
  private TicketService service;

  @BeforeEach
  void setUp() {
    orders = mock(OrderRepository.class);
    tickets = mock(TicketRepository.class);
    service = new TicketService(orders, tickets);
    when(tickets.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void 본인_주문의_환불은_PENDING_티켓만_생성한다() {
    Order order = order("12345", "user1");
    when(orders.findOwnedByIdForUpdate("12345", "user1")).thenReturn(Optional.of(order));

    TicketView result = service.requestRefund("12345", "user1", "단순 변심");

    assertThat(result.status()).isEqualTo("PENDING");
    assertThat(result.message()).contains("승인 후");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
    verify(tickets).save(any(Ticket.class));
  }

  @Test
  void 남의_주문에는_환불_티켓을_생성하지_않는다() {
    when(orders.findOwnedByIdForUpdate("99999", "user1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requestRefund("99999", "user1", "환불"))
        .isInstanceOf(OrderNotFoundException.class);
    verify(tickets, never()).save(any());
  }

  @Test
  void 본인_주문의_교환은_EXCHANGE_PENDING_티켓을_생성한다() {
    Order order = order("12345", "user1");
    when(orders.findOwnedByIdForUpdate("12345", "user1")).thenReturn(Optional.of(order));

    TicketView result = service.requestExchange("12345", "user1", "색상 변경");

    assertThat(result.type()).isEqualTo("EXCHANGE");
    assertThat(result.status()).isEqualTo("PENDING");
    assertThat(result.message()).contains("승인 후");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
  }

  @Test
  void 남의_주문에는_교환_티켓도_생성하지_않는다() {
    when(orders.findOwnedByIdForUpdate("99999", "user1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requestExchange("99999", "user1", "색상 변경"))
        .isInstanceOf(OrderNotFoundException.class);
    verify(tickets, never()).save(any());
  }

  @Test
  void 승인은_별도_서비스_호출에서만_상태를_변경한다() {
    Ticket ticket = new Ticket("T1", "12345", "user1", TicketType.REFUND, "단순 변심", Instant.now());
    when(tickets.findById("T1")).thenReturn(Optional.of(ticket));

    TicketView result = service.approve("T1");

    assertThat(result.status()).isEqualTo("APPROVED");
    assertThat(ticket.getApprovedAt()).isNotNull();
  }

  @Test
  void 존재하지_않는_티켓은_승인할_수_없다() {
    when(tickets.findById("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.approve("missing"))
        .isInstanceOf(TicketNotFoundException.class);
  }

  @Test
  void 이미_승인된_티켓은_다시_승인할_수_없다() {
    Ticket ticket = new Ticket("T1", "12345", "user1", TicketType.REFUND, "단순 변심", Instant.now());
    ticket.approve(Instant.now());
    when(tickets.findById("T1")).thenReturn(Optional.of(ticket));

    assertThatThrownBy(() -> service.approve("T1"))
        .isInstanceOf(TicketAlreadyApprovedException.class);
  }

  @Test
  void 동일한_PENDING_요청은_기존_티켓을_반환한다() {
    Order order = order("12345", "user1");
    Ticket existing = new Ticket("T1", "12345", "user1", TicketType.REFUND, "첫 요청", Instant.now());
    when(orders.findOwnedByIdForUpdate("12345", "user1")).thenReturn(Optional.of(order));
    when(tickets.findFirstByOrderIdAndUserIdAndTypeAndStatus(
        "12345", "user1", TicketType.REFUND, TicketStatus.PENDING)).thenReturn(Optional.of(existing));

    TicketView result = service.requestRefund("12345", "user1", "다시 요청");

    assertThat(result.ticketNo()).isEqualTo("T1");
    assertThat(result.message()).contains("이미", "승인 대기");
    verify(tickets, never()).save(any());
  }

  @Test
  void 사유가_없으면_사용자_요청으로_저장한다() {
    when(orders.findOwnedByIdForUpdate("12345", "user1"))
        .thenReturn(Optional.of(order("12345", "user1")));

    service.requestExchange("12345", "user1", " ");

    verify(tickets).save(org.mockito.ArgumentMatchers.argThat(
        ticket -> ticket.getReason().equals("사용자 요청")));
  }

  @Test
  void 본인_주문의_최신_티켓_상태를_조회한다() {
    Ticket approved = new Ticket("T1", "12345", "user1", TicketType.EXCHANGE,
        "사용자 요청", Instant.now());
    approved.approve(Instant.now());
    when(orders.findByIdAndOwnerId("12345", "user1"))
        .thenReturn(Optional.of(order("12345", "user1")));
    when(tickets.findFirstByOrderIdAndUserIdOrderByCreatedAtDesc("12345", "user1"))
        .thenReturn(Optional.of(approved));

    TicketView result = service.latestForOrder("12345", "user1");

    assertThat(result.status()).isEqualTo("APPROVED");
    assertThat(result.message()).contains("승인");
  }

  @Test
  void 다른_사용자의_주문_티켓은_조회하지_못한다() {
    when(orders.findByIdAndOwnerId("12345", "user2")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.latestForOrder("12345", "user2"))
        .isInstanceOf(OrderNotFoundException.class);
    verify(tickets, never()).findFirstByOrderIdAndUserIdOrderByCreatedAtDesc(any(), any());
  }

  private Order order(String id, String ownerId) {
    return new Order(id, ownerId, "무선 이어폰", "2026-08-20", OrderStatus.SHIPPING);
  }
}
