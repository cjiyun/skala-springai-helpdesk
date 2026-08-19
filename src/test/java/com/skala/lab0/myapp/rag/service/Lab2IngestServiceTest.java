package com.skala.lab0.myapp.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;

import com.skala.lab0.myapp.rag.dto.Lab2IngestResponse;

class Lab2IngestServiceTest {
  @Test
  void 재인제스트하면_해당_source만_최신_내용으로_교체한다() {
    EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
    when(embeddingModel.embed(any(Document.class))).thenReturn(new float[] {1, 0});
    when(embeddingModel.embed(anyString())).thenReturn(new float[] {1, 0});
    VectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
    Lab2IngestService service = new Lab2IngestService(vectorStore);

    service.ingest(new ClassPathResource("lab2-docs/return-policy.md"), "policy");
    Lab2IngestResponse membership = service.ingest(
        new ClassPathResource("lab2-docs/membership.md"), "membership");
    Lab2IngestResponse reindexed = service.ingest(
        new ClassPathResource("lab2-docs/shipping-policy.md"), "policy");

    List<Document> stored = vectorStore.similaritySearch(SearchRequest.builder()
        .query("반품 기한")
        .topK(100)
        .similarityThreshold(0)
        .build());

    assertThat(stored).hasSize(reindexed.chunks() + membership.chunks());
    assertThat(stored).extracting(document -> document.getText())
        .noneMatch(text -> text.contains("단순 변심"))
        .anyMatch(text -> text.contains("추가 배송비"))
        .anyMatch(text -> text.contains("실버 등급"));
    assertThat(stored).extracting(document -> document.getMetadata().get("source"))
        .containsExactlyInAnyOrder("policy", "membership");
  }
}
