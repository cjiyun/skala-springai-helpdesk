package com.skala.lab0.myapp.lab3.ticket;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refund_tickets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket {
  @Id
  private String id;

  @Column(nullable = false)
  private String orderId;

  @Column(nullable = false)
  private String userId;

  @Column(nullable = false)
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TicketStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  private Instant approvedAt;

  public Ticket(String id, String orderId, String userId, String reason, Instant createdAt) {
    this.id = id;
    this.orderId = orderId;
    this.userId = userId;
    this.reason = reason;
    this.status = TicketStatus.PENDING;
    this.createdAt = createdAt;
  }

  public void approve(Instant approvedAt) {
    if (status != TicketStatus.PENDING) {
      throw new TicketAlreadyApprovedException();
    }
    this.status = TicketStatus.APPROVED;
    this.approvedAt = approvedAt;
  }
}
