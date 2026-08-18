package com.skala.lab0.myapp.domain;

public enum OrderStatus {
    ORDERED("주문 완료"),
    PREPARING("상품 준비 중"),
    SHIPPING("배송 중"),
    DELIVERED("배송 완료"),
    CANCELED("주문 취소");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}