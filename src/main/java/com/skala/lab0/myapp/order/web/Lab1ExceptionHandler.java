package com.skala.lab0.myapp.order.web;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.skala.lab0.myapp.order.dto.ErrorResponse;
import com.skala.lab0.myapp.order.service.OrderNotFoundException;

@RestControllerAdvice
public class Lab1ExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(Lab1ExceptionHandler.class);

  @ExceptionHandler(OrderNotFoundException.class)
  public ResponseEntity<ErrorResponse> notFound(
      OrderNotFoundException exception) {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse(
            "주문을 찾을 수 없습니다.",
            null));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> unexpected(
      Exception exception) {
    String traceId = UUID.randomUUID()
        .toString()
        .substring(0, 8);

    log.error("[{}] 요청 처리 실패", traceId, exception);

    return ResponseEntity
        .status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new ErrorResponse(
            "요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.",
            traceId));
  }
}