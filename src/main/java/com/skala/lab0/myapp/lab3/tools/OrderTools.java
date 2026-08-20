package com.skala.lab0.myapp.lab3.tools;

import java.util.Objects;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.skala.lab0.myapp.lab3.order.OrderView;
import com.skala.lab0.myapp.lab3.ticket.TicketService;
import com.skala.lab0.myapp.lab3.ticket.TicketView;
import com.skala.lab0.myapp.order.repository.OrderRepository;
import com.skala.lab0.myapp.order.service.OrderNotFoundException;

@Component
public class OrderTools {
  private final OrderRepository orders;
  private final TicketService tickets;

  public OrderTools(OrderRepository orders, TicketService tickets) {
    this.orders = orders;
    this.tickets = tickets;
  }

  @Tool(description = """
      사용자가 주문번호를 말하며 자신의 주문 상태나 예상 도착일을 물을 때 사용한다.
      예: '제 주문 12345는 어디쯤이에요?', '12345 배송은 언제 와요?'
      주문번호가 없으면 이 도구를 호출하지 말고 사용자에게 주문번호를 물어본다.
      """)
  public OrderView getOrder(
      @ToolParam(description = "조회할 주문번호. 예: 12345") String orderId,
      ToolContext context) {
    String userId = userId(context);
    String requiredOrderId = required(orderId, "orderId");
    return orders.findByIdAndOwnerId(requiredOrderId, userId)
        .map(OrderView::from)
        .orElseThrow(() -> new OrderNotFoundException(requiredOrderId));
  }

  @Tool(description = """
      사용자가 주문번호와 환불 사유를 말하며 자신의 주문 환불 접수를 요청할 때 사용한다.
      환불을 즉시 처리하지 않고 관리자 승인 대기(PENDING) 티켓만 생성한다.
      예: '주문 12345를 단순 변심으로 환불 접수해 주세요.'
      주문번호나 사유가 없으면 도구를 호출하지 말고 누락된 정보를 물어본다.
      """)
  public TicketView requestRefund(
      @ToolParam(description = "환불 접수할 주문번호. 예: 12345") String orderId,
      @ToolParam(description = "사용자가 말한 환불 사유") String reason,
      ToolContext context) {
    return tickets.requestRefund(orderId, userId(context), reason);
  }

  private String userId(ToolContext context) {
    Object value = context == null ? null : context.getContext().get("userId");
    String userId = Objects.toString(value, "").trim();
    if (userId.isEmpty()) {
      throw new IllegalArgumentException("ToolContext에 userId가 필요합니다.");
    }
    return userId;
  }

  private String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.trim();
  }
}
