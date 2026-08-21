package com.skala.lab0.myapp.rag.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import com.skala.lab0.myapp.rag.dto.Lab2ChunkResponse;

@Service
public class Lab2SearchService {
  private final VectorStore vectorStore;

  @Value("${lab2.rag.search.min-score:0.29}")
  private double minScore = 0.29;

  public Lab2SearchService(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  public List<Lab2ChunkResponse> retrieve(String question, int topK) {
    return vectorStore.similaritySearch(SearchRequest.builder()
        .query(question)
        .topK(topK)
        .similarityThreshold(minScore)
        .build())
        .stream()
        .map(document -> new Lab2ChunkResponse(
            String.valueOf(document.getMetadata().get("source")),
            String.valueOf(document.getMetadata().getOrDefault("version", "unknown")),
            document.getScore() == null ? 0 : document.getScore(),
            snippet(document.getText())))
        .toList();
  }

  private String snippet(String text) {
    return text.length() <= 120 ? text : text.substring(0, 120) + "...";
  }
}
