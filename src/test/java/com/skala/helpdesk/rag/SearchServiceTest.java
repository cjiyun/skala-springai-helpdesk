package com.skala.helpdesk.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import com.skala.helpdesk.rag.dto.ChunkResponse;

class SearchServiceTest {
  @Test
  void 검색_결과에_출처_점수_snippet을_반환한다() {
    VectorStore vectorStore = mock(VectorStore.class);
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
        Document.builder()
            .text("단순 변심 반품은 상품 수령 후 7일 이내입니다.")
            .metadata(Map.of("source", "return-policy", "version", "2026-08-21"))
            .score(0.91)
            .build()));

    List<ChunkResponse> results = new SearchService(vectorStore)
        .retrieve("반품 기한", 4);

    assertThat(results).singleElement().satisfies(result -> {
      assertThat(result.source()).isEqualTo("return-policy");
      assertThat(result.version()).isEqualTo("2026-08-21");
      assertThat(result.score()).isEqualTo(0.91);
      assertThat(result.snippet()).contains("7일");
    });
    verify(vectorStore).similaritySearch(any(SearchRequest.class));
  }
}
