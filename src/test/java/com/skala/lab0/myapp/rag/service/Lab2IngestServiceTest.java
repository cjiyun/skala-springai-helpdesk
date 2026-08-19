package com.skala.lab0.myapp.rag.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;

import com.skala.lab0.myapp.rag.dto.Lab2IngestResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class Lab2IngestServiceTest {
  @Test
  void 같은_문서를_다시_인제스트하면_기존_source를_삭제하고_저장한다() {
    VectorStore vectorStore = mock(VectorStore.class);
    Lab2IngestService service = new Lab2IngestService(vectorStore);

    Lab2IngestResponse result = service.ingest(
        new ClassPathResource("lab2-docs/return-policy.md"), "return-policy");

    assertThat(result.source()).isEqualTo("return-policy");
    assertThat(result.chunks()).isGreaterThan(0);
    verify(vectorStore, times(1)).delete("source == 'return-policy'");
    verify(vectorStore, times(1)).add(argThat(documents ->
        documents.stream().allMatch(document -> "return-policy".equals(document.getMetadata().get("source")))));
  }
}