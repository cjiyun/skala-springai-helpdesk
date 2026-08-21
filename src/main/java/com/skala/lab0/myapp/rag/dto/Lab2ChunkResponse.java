package com.skala.lab0.myapp.rag.dto;

public record Lab2ChunkResponse(String source, String version, double score, String preview) {
  public Lab2ChunkResponse(String source, double score, String preview) {
    this(source, "unknown", score, preview);
  }

  public String snippet() { return preview; }
}
