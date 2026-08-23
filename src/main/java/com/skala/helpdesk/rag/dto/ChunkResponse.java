package com.skala.helpdesk.rag.dto;

public record ChunkResponse(String source, String version, double score, String preview) {
  public ChunkResponse(String source, double score, String preview) {
    this(source, "unknown", score, preview);
  }

  public String snippet() { return preview; }
}
