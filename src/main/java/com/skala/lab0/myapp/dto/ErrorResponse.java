package com.skala.lab0.myapp.dto;

public record ErrorResponse(
    String message,
    String traceId
) {
}