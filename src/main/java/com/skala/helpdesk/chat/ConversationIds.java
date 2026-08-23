package com.skala.helpdesk.chat;

public final class ConversationIds {
  private ConversationIds() {}

  public static String of(String tenantId, String userId, String sessionId) {
    return required(tenantId, "tenantId") + ":" + required(userId, "userId") + ":" + required(sessionId, "sessionId");
  }

  private static String required(String value, String name) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
    return value.trim();
  }
}
