package com.skala.helpdesk.tools;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.skala.helpdesk.dto.TicketView;
import com.skala.helpdesk.service.TicketService;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class TicketTools {
  private static final Logger log = LoggerFactory.getLogger(TicketTools.class);

  private final TicketService tickets;
  private final MeterRegistry meterRegistry;

  public TicketTools(TicketService tickets, MeterRegistry meterRegistry) {
    this.tickets = tickets;
    this.meterRegistry = meterRegistry;
  }

  @Tool(description = """
      사용자가 이전에 접수한 교환·환불 티켓의 처리 상태를 주문번호로 조회할 때 사용한다.
      주문번호가 없으면 사용자에게 물어본다.
      """)
  public TicketView getTicketStatus(
      @ToolParam(description = "티켓을 조회할 주문번호. 예: 12345") String orderId,
      ToolContext context) {
    Timer.Sample sample = Timer.start(meterRegistry);
    try {
      markUsed(context);
      String requiredOrderId = required(orderId, "orderId");
      TicketView result = tickets.latestForOrder(requiredOrderId, userId(context));
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
      사용자의 주문 환불 요청을 관리자 승인 대기(PENDING) 티켓으로 접수한다.
      주문번호가 없으면 사용자에게 물어보고, 사유가 없으면 '사용자 요청'을 사용한다.
      """)
  public TicketView requestRefund(
      @ToolParam(description = "환불 접수할 주문번호. 예: 12345") String orderId,
      @ToolParam(required = false, description = "환불 사유") String reason,
      ToolContext context) {
    return request("requestRefund", orderId, reason, context, true);
  }

  @Tool(description = """
      사용자의 주문 교환 요청을 관리자 승인 대기(PENDING) 티켓으로 접수한다.
      주문번호가 없으면 사용자에게 물어보고, 사유가 없으면 '사용자 요청'을 사용한다.
      """)
  public TicketView requestExchange(
      @ToolParam(description = "교환 접수할 주문번호. 예: 12345") String orderId,
      @ToolParam(required = false, description = "교환 사유") String reason,
      ToolContext context) {
    return request("requestExchange", orderId, reason, context, false);
  }

  private TicketView request(String tool, String orderId, String reason,
      ToolContext context, boolean refund) {
    Timer.Sample sample = Timer.start(meterRegistry);
    try {
      markWriteUsed(context);
      TicketView result = refund
          ? tickets.requestRefund(orderId, userId(context), reason)
          : tickets.requestExchange(orderId, userId(context), reason);
      record(tool, "ok", sample);
      log.info("[AUDIT-TOOL] [traceId={}] tool={} result=ok orderId={}",
          MDC.get("traceId"), tool, orderId);
      return result;
    } catch (RuntimeException exception) {
      record(tool, "fail", sample);
      log.warn("[AUDIT-TOOL] [traceId={}] tool={} result=fail errorType={}",
          MDC.get("traceId"), tool, exception.getClass().getSimpleName());
      throw exception;
    }
  }

  private void record(String tool, String result, Timer.Sample sample) {
    meterRegistry.counter("ai.tool.calls", "tool", tool, "result", result).increment();
    sample.stop(Timer.builder("ai.latency")
        .tag("phase", "tool").tag("tool", tool).tag("result", result)
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

  private String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.trim();
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
}
