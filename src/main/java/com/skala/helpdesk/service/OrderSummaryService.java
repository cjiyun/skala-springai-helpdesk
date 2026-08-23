package com.skala.helpdesk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.skala.helpdesk.domain.Order;
import com.skala.helpdesk.dto.SummaryResponse;
import com.skala.helpdesk.repository.OrderRepository;

@Service
@Transactional(readOnly = true)
public class OrderSummaryService {

  private static final Logger log = LoggerFactory.getLogger(OrderSummaryService.class);

  private final OrderRepository orders;
  private final ChatClient summaryChat;

  public OrderSummaryService(OrderRepository orders, ChatClient summaryChatClient) {
    this.orders = orders;
    this.summaryChat = summaryChatClient;
  }

  public SummaryResponse summarize(String orderId, String userId) {
    Order order = orders.findByIdAndOwnerId(orderId, userId) // 권한은 쿼리 안에
        .orElseThrow(() -> new OrderNotFoundException(orderId));

    String summary;

    try {
      summary = callModel(order);
    } catch (Exception exception) {
      Throwable rootCause = exception;

      while (rootCause.getCause() != null) {
        rootCause = rootCause.getCause();
      }

      log.error(
          "AI 요약 호출 실패: type={}, message={}",
          rootCause.getClass().getName(),
          rootCause.getMessage());

      summary = createFallbackSummary(order);
    }

    return new SummaryResponse(order.getId(), summary);
  }

  private String callModel(Order order) {
    return summaryChat.prompt()
        .user(user -> user
            .text("""
                주문번호 {id} · 상품 {item} · 상태 {status} · 도착예정 {eta}
                위 정보를 한 문장으로 요약해 줘.
                """)
            .param("id", order.getId())
            .param("item", order.getItem())
            .param("status", order.getStatus().label())
            .param("eta", order.getEta()))
        .call()
        .content();
  }

  private String createFallbackSummary(Order order) {
    return order.getItem() + " · " + order.getStatus().label();
  }
}