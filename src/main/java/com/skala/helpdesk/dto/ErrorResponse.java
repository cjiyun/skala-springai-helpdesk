package com.skala.helpdesk.dto;

public record ErrorResponse(
    String message,
    String traceId
) {
}