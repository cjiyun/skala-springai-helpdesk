package com.skala.lab0.myapp.lab3.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import com.skala.lab0.myapp.lab3.order.OrderView;
import com.skala.lab0.myapp.lab3.ticket.TicketService;
import com.skala.lab0.myapp.lab3.ticket.TicketView;
import com.skala.lab0.myapp.order.domain.Order;
import com.skala.lab0.myapp.order.domain.OrderStatus;
import com.skala.lab0.myapp.order.repository.OrderRepository;
import com.skala.lab0.myapp.order.service.OrderNotFoundException;

class OrderToolsTest {
  private OrderRepository orders;
  private TicketService tickets;
  private OrderTools tools;
  private SimpleMeterRegistry meterRegistry;

  @BeforeEach
  void setUp() {
    orders = mock(OrderRepository.class);
    tickets = mock(TicketService.class);
    meterRegistry = new SimpleMeterRegistry();
    tools = new OrderTools(orders, tickets, meterRegistry);
  }

  @Test
  void ToolContext의_사용자로_본인_주문을_조회한다() {
    when(orders.findByIdAndOwnerId("12345", "user1"))
        .thenReturn(Optional.of(order("12345", "user1")));

    OrderView result = tools.getOrder("12345", context("user1"));

    assertThat(result.orderId()).isEqualTo("12345");
    assertThat(result.status()).isEqualTo("배송 중");
    verify(orders).findByIdAndOwnerId("12345", "user1");
    assertThat(meterRegistry.get("ai.tool.calls")
        .tag("tool", "getOrder")
        .tag("result", "ok")
        .counter().count()).isEqualTo(1);
  }

  @Test
  void 다른_사용자의_주문은_찾을_수_없다() {
    when(orders.findByIdAndOwnerId("99999", "user1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tools.getOrder("99999", context("user1")))
        .isInstanceOf(OrderNotFoundException.class);
    verify(orders).findByIdAndOwnerId("99999", "user1");
    assertThat(meterRegistry.get("ai.tool.calls")
        .tag("tool", "getOrder")
        .tag("result", "fail")
        .counter().count()).isEqualTo(1);
  }

  @Test
  void 질문의_사용자_ID가_아닌_ToolContext만_신뢰한다() {
    when(orders.findByIdAndOwnerId("user2-99999", "user1")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> tools.getOrder("user2-99999", context("user1")))
        .isInstanceOf(OrderNotFoundException.class);
    verify(orders).findByIdAndOwnerId("user2-99999", "user1");
  }

  @Test
  void ToolContext에_사용자가_없으면_도구를_실행하지_않는다() {
    assertThatThrownBy(() -> tools.getOrder("12345", new ToolContext(Map.of())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("userId");
  }

  @Test
  void 주문번호가_없으면_도구를_실행하지_않는다() {
    assertThatThrownBy(() -> tools.getOrder(" ", context("user1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("orderId");
  }

  @Test
  void 환불_접수에도_ToolContext의_사용자를_전달한다() {
    TicketView pending = new TicketView("T1", "12345", "PENDING", "승인 대기");
    when(tickets.requestRefund("12345", "user1", "단순 변심")).thenReturn(pending);

    TicketView result = tools.requestRefund("12345", "단순 변심", context("user1"));

    assertThat(result.status()).isEqualTo("PENDING");
    verify(tickets).requestRefund("12345", "user1", "단순 변심");
  }

  private ToolContext context(String userId) {
    return new ToolContext(Map.of("userId", userId));
  }

  private Order order(String id, String ownerId) {
    return new Order(id, ownerId, "무선 이어폰", "2026-08-20", OrderStatus.SHIPPING);
  }
}
