package com.skala.lab0.myapp.lab3.order;

import com.skala.lab0.myapp.order.domain.Order;

public record OrderView(String orderId, String item, String status, String eta) {
  public static OrderView from(Order order) {
    return new OrderView(
        order.getId(),
        order.getItem(),
        order.getStatus().label(),
        order.getEta());
  }
}
