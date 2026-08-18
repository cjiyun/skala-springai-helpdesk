package com.skala.lab0.myapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
  @Id
  private String id;

  private String ownerId;
  private String item;
  private String eta;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;
}
