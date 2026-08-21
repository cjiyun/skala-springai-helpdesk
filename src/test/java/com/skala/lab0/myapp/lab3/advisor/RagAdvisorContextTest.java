package com.skala.lab0.myapp.lab3.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class RagAdvisorContextTest {
  @Test
  void 검색_근거가_없어도_RAG_시도_여부와_빈_출처를_남긴다() {
    VectorStore vectorStore = mock(VectorStore.class);
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
    CallAdvisorChain chain = mock(CallAdvisorChain.class);
    when(chain.nextCall(any())).thenReturn(ChatClientResponse.builder().build());
    ChatClientRequest request = ChatClientRequest.builder()
        .prompt(new Prompt(new UserMessage("출장 규정 알려줘")))
        .build();

    ChatClientResponse response = new RagAdvisor(vectorStore, 5, 0.62).adviseCall(request, chain);

    assertThat(response.context())
        .containsEntry(RagAdvisor.RAG_ATTEMPTED, true)
        .containsEntry(RagAdvisor.RETRIEVED_DOCUMENTS, List.of());
  }
}
