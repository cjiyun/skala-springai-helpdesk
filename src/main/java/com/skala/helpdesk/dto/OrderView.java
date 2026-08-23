package com.skala.helpdesk.dto;

import com.skala.helpdesk.domain.Order;

public record OrderView(String orderId, String item, String status, String eta) {
  public static OrderView from(Order order) {
    return new OrderView(
        order.getId(),
        order.getItem(),
        order.getStatus().label(),
        order.getEta());
  }
}
