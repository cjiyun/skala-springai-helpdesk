package com.skala.lab0.myapp.lab3.tools;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.skala.lab0.myapp.lab3.order.OrderView;
import com.skala.lab0.myapp.lab3.ticket.TicketService;
import com.skala.lab0.myapp.lab3.ticket.TicketView;
import com.skala.lab0.myapp.order.repository.OrderRepository;
import com.skala.lab0.myapp.order.service.OrderNotFoundException;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class OrderTools {
  private static final Logger log = LoggerFactory.getLogger(OrderTools.class);

  private final OrderRepository orders;
  private final TicketService tickets;
  private final MeterRegistry meterRegistry;

  public OrderTools(OrderRepository orders, TicketService tickets, MeterRegistry meterRegistry) {
    this.orders = orders;
    this.tickets = tickets;
    this.meterRegistry = meterRegistry;
  }

  @Tool(description = """
      사용자가 주문번호를 말하며 자신의 주문 상태나 예상 도착일을 물을 때 사용한다.
      예: '제 주문 12345는 어디쯤이에요?', '12345 배송은 언제 와요?'
      현재 질문과 같은 대화의 이전 발화에도 주문번호가 없으면 사용자에게 주문번호를 물어본다.
      """)
  public OrderView getOrder(
      @ToolParam(description = "조회할 주문번호. 예: 12345") String orderId,
      ToolContext context) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String userId = null;
    try {
      markUsed(context);
      userId = userId(context);
      String requiredOrderId = required(orderId, "orderId");
      OrderView result = orders.findByIdAndOwnerId(requiredOrderId, userId)
          .map(OrderView::from)
          .orElseThrow(() -> new OrderNotFoundException(requiredOrderId));
      record("getOrder", "ok", sample);
      log.info("[AUDIT-TOOL] [traceId={}] tool=getOrder result=ok orderId={}",
          MDC.get("traceId"), requiredOrderId);
      return result;
    } catch (RuntimeException exception) {
      record("getOrder", "fail", sample);
      log.warn("[AUDIT-TOOL] [traceId={}] tool=getOrder result=fail errorType={}",
          MDC.get("traceId"), exception.getClass().getSimpleName());
      throw exception;
    }
  }

  @Tool(description = """
      사용자가 이전에 접수한 교환·환불 티켓의 처리 상태를 주문번호로 조회할 때 사용한다.
      예: '주문 12345 교환 접수는 어떻게 됐어요?', '12345 환불 티켓 상태를 알려 주세요.'
      주문번호가 없으면 사용자에게 물어보고, 주문 조회 도구 대신 티켓 상태를 반환한다.
      """)
  public TicketView getTicketStatus(
      @ToolParam(description = "티켓을 조회할 주문번호. 예: 12345") String orderId,
      ToolContext context) {
    Timer.Sample sample = Timer.start(meterRegistry);
    try {
      markUsed(context);
      String userId = userId(context);
      String requiredOrderId = required(orderId, "orderId");
      TicketView result = tickets.latestForOrder(requiredOrderId, userId);
      record("getTicketStatus", "ok", sample);
      log.info("[AUDIT-TOOL] [traceId={}] tool=getTicketStatus result=ok orderId={}",
          MDC.get("traceId"), requiredOrderId);
      return result;
    } catch (RuntimeException exception) {
      record("getTicketStatus", "fail", sample);
      log.warn("[AUDIT-TOOL] [traceId={}] tool=getTicketStatus result=fail errorType={}",
          MDC.get("traceId"), exception.getClass().getSimpleName());
      throw exception;
    }
  }

  @Tool(description = """
      사용자가 주문번호와 환불 사유를 말하며 자신의 주문 환불 접수를 요청할 때 사용한다.
      환불을 즉시 처리하지 않고 관리자 승인 대기(PENDING) 티켓만 생성한다.
      예: '주문 12345를 단순 변심으로 환불 접수해 주세요.'
      주문번호가 없으면 사용자에게 물어본다. 사유만 없으면 '사용자 요청'을 사용한다.
      """)
  public TicketView requestRefund(
      @ToolParam(description = "환불 접수할 주문번호. 예: 12345") String orderId,
      @ToolParam(required = false, description = "사용자가 말한 환불 사유. 생략하면 '사용자 요청'") String reason,
      ToolContext context) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String userId = null;
    try {
      markWriteUsed(context);
      userId = userId(context);
      TicketView result = tickets.requestRefund(orderId, userId, reason);
      record("requestRefund", "ok", sample);
      log.info("[AUDIT-TOOL] [traceId={}] tool=requestRefund result=ok orderId={}",
          MDC.get("traceId"), orderId);
      return result;
    } catch (RuntimeException exception) {
      record("requestRefund", "fail", sample);
      log.warn("[AUDIT-TOOL] [traceId={}] tool=requestRefund result=fail errorType={}",
          MDC.get("traceId"), exception.getClass().getSimpleName());
      throw exception;
    }
  }

  @Tool(description = """
      사용자가 주문번호와 교환 사유를 말하며 자신의 주문 교환 접수를 요청할 때 사용한다.
      환불 도구와 달리 EXCHANGE 유형의 관리자 승인 대기(PENDING) 티켓만 생성하며 실제 교환은 처리하지 않는다.
      예: '주문 12345를 색상 변경 사유로 교환 접수해 주세요.'
      주문번호가 없으면 사용자에게 물어본다. 사유만 없으면 '사용자 요청'을 사용한다.
      """)
  public TicketView requestExchange(
      @ToolParam(description = "교환 접수할 주문번호. 예: 12345") String orderId,
      @ToolParam(required = false, description = "교환 사유. 생략하면 '사용자 요청'") String reason,
      ToolContext context) {
    Timer.Sample sample = Timer.start(meterRegistry);
    String userId = null;
    try {
      markWriteUsed(context);
      userId = userId(context);
      TicketView result = tickets.requestExchange(orderId, userId, reason);
      record("requestExchange", "ok", sample);
      log.info("[AUDIT-TOOL] [traceId={}] tool=requestExchange result=ok orderId={}",
          MDC.get("traceId"), orderId);
      return result;
    } catch (RuntimeException exception) {
      record("requestExchange", "fail", sample);
      log.warn("[AUDIT-TOOL] [traceId={}] tool=requestExchange result=fail errorType={}",
          MDC.get("traceId"), exception.getClass().getSimpleName());
      throw exception;
    }
  }

  private void record(String tool, String result, Timer.Sample sample) {
    meterRegistry.counter("ai.tool.calls", "tool", tool, "result", result).increment();
    sample.stop(Timer.builder("ai.latency")
        .tag("phase", "tool")
        .tag("tool", tool)
        .tag("result", result)
        .register(meterRegistry));
  }

  private String userId(ToolContext context) {
    Object value = context == null ? null : context.getContext().get("userId");
    String userId = Objects.toString(value, "").trim();
    if (userId.isEmpty()) {
      throw new IllegalArgumentException("ToolContext에 userId가 필요합니다.");
    }
    return userId;
  }

  private void markUsed(ToolContext context) {
    if (context != null && context.getContext().get(ToolUsage.CONTEXT_KEY) instanceof ToolUsage usage) {
      usage.markUsed();
    }
  }

  private void markWriteUsed(ToolContext context) {
    if (context != null && context.getContext().get(ToolUsage.CONTEXT_KEY) instanceof ToolUsage usage) {
      usage.markWriteUsed();
    }
  }

  private String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.trim();
  }
}
