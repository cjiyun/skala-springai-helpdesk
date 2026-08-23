package com.skala.helpdesk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "helpdesk")
public record HelpDeskProperties(
    Rag rag,
    Memory memory,
    Model model,
    Cost cost,
    String tenantId) {

  public HelpDeskProperties {
    rag = rag == null ? new Rag(2, 0.29) : rag;
    memory = memory == null ? new Memory(20) : memory;
    model = model == null ? new Model("gpt-4.1-mini") : model;
    cost = cost == null
        ? new Cost(new Rates(0.15, 0.60), new Rates(0.40, 1.60))
        : cost;
    tenantId = tenantId == null || tenantId.isBlank() ? "skala" : tenantId;
  }

  public record Rag(int topK, double threshold) {}

  public record Memory(int max) {}

  public record Model(String fallback) {}

  public record Cost(Rates primary, Rates fallback) {}

  public record Rates(double inputPerMillionUsd, double outputPerMillionUsd) {}
}
