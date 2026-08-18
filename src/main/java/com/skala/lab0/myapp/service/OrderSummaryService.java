package com.skala.lab0.myapp.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import com.skala.lab0.myapp.domain.Order;
import com.skala.lab0.myapp.dto.SummaryResponse;
import com.skala.lab0.myapp.repository.OrderRepository;

@Service
@Transactional(readOnly = true)
public class OrderSummaryService {
  private final OrderRepository orders; // 1장에서 만든 계층을 그로
  private final ChatClient summaryChat;
  
  public OrderSummaryService(OrderRepository orders, ChatClient summaryChatClient) {
    this.orders = orders;
    this.summaryChat = summaryChatClient;
  }
  
  public SummaryResponse summarize(String orderId, String userId) {
    Order order = orders.findByIdAndOwnerId(orderId, userId) // 권한은 쿼리 안에
      .orElseThrow(() -> new OrderNotFoundException(orderId));
    
    String summary = summaryChat.prompt()
      .user(u -> u.text("주문번호 {id} · 상품 {item} · 상태 {status} · 도착예정 {eta}"
        + "\n위 정보를 한 문장으로 요약해 줘.")
        .param("id", order.getId()).param("item", order.getItem())
        .param("status", order.getStatus().label()).param("eta", order.getEta()))
      .call().content();
    return new SummaryResponse(order.getId(), summary);
  }
}