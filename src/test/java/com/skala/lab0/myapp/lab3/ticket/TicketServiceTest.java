package com.skala.lab0.myapp.lab3.ticket;

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

import com.skala.lab0.myapp.order.domain.Order;
import com.skala.lab0.myapp.order.domain.OrderStatus;
import com.skala.lab0.myapp.order.repository.OrderRepository;
import com.skala.lab0.myapp.order.service.OrderNotFoundException;

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
    when(orders.findByIdAndOwnerId("12345", "user1")).thenReturn(Optional.of(order));

    TicketView result = service.requestRefund("12345", "user1", "단순 변심");

    assertThat(result.status()).isEqualTo("PENDING");
    assertThat(result.message()).contains("승인 후");
    assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
    verify(tickets).save(any(Ticket.class));
  }

  @Test
  void 남의_주문에는_환불_티켓을_생성하지_않는다() {
    when(orders.findByIdAndOwnerId("99999", "user1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.requestRefund("99999", "user1", "환불"))
        .isInstanceOf(OrderNotFoundException.class);
    verify(tickets, never()).save(any());
  }

  @Test
  void 승인은_별도_서비스_호출에서만_상태를_변경한다() {
    Ticket ticket = new Ticket("T1", "12345", "user1", "단순 변심", Instant.now());
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
    Ticket ticket = new Ticket("T1", "12345", "user1", "단순 변심", Instant.now());
    ticket.approve(Instant.now());
    when(tickets.findById("T1")).thenReturn(Optional.of(ticket));

    assertThatThrownBy(() -> service.approve("T1"))
        .isInstanceOf(TicketAlreadyApprovedException.class);
  }

  private Order order(String id, String ownerId) {
    return new Order(id, ownerId, "무선 이어폰", "2026-08-20", OrderStatus.SHIPPING);
  }
}
