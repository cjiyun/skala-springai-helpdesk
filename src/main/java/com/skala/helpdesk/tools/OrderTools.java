package com.skala.helpdesk.tools;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.skala.helpdesk.dto.OrderView;
import com.skala.helpdesk.repository.OrderRepository;
import com.skala.helpdesk.service.OrderNotFoundException;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class OrderTools {
  private static final Logger log = LoggerFactory.getLogger(OrderTools.class);

  private final OrderRepository orders;
  private final MeterRegistry meterRegistry;

  public OrderTools(OrderRepository orders, MeterRegistry meterRegistry) {
    this.orders = orders;
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

  private String required(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.trim();
  }
}
