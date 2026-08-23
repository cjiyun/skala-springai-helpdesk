package com.skala.helpdesk.chat;

import java.util.List;

public record HistoryDto(List<HistoryMessage> history) {
  public record HistoryMessage(String role, String content, String toolName, String status) {
    public static HistoryMessage message(String role, String content) {
      return new HistoryMessage(role, content, null, null);
    }

    public static HistoryMessage tool(String toolName, String status) {
      return new HistoryMessage("TOOL", null, toolName, status);
    }
  }
}
