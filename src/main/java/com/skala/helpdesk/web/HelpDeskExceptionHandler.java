package com.skala.helpdesk.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.skala.helpdesk.dto.ErrorResponse;
import com.skala.helpdesk.service.TicketAlreadyApprovedException;
import com.skala.helpdesk.service.TicketNotFoundException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {AdminController.class, ChatController.class})
public class HelpDeskExceptionHandler {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> badRequest(ResponseStatusException exception) {
    return ResponseEntity
        .status(exception.getStatusCode())
        .body(new ErrorResponse(exception.getReason(), null));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> invalidRequest(IllegalArgumentException exception) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse(exception.getMessage(), null));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> forbidden() {
    return ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse("관리자 권한이 필요합니다.", null));
  }

  @ExceptionHandler(TicketNotFoundException.class)
  public ResponseEntity<ErrorResponse> ticketNotFound() {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(new ErrorResponse("티켓을 찾을 수 없습니다.", null));
  }

  @ExceptionHandler(TicketAlreadyApprovedException.class)
  public ResponseEntity<ErrorResponse> alreadyApproved() {
    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("이미 승인된 티켓입니다.", null));
  }
}
