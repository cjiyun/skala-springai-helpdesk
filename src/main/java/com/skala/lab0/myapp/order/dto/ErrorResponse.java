package com.skala.lab0.myapp.order.dto;

public record ErrorResponse(
    String message,
    String traceId
) {
}