package com.skala.helpdesk.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.ArgumentCaptor;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
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

  @Test
  void 맥락_없는_진행상황_후속질문은_RAG로_오분류하지_않는다() {
    VectorStore vectorStore = mock(VectorStore.class);
    CallAdvisorChain chain = mock(CallAdvisorChain.class);
    ChatClientResponse downstream = ChatClientResponse.builder().build();
    when(chain.nextCall(any())).thenReturn(downstream);
    ChatClientRequest request = ChatClientRequest.builder()
        .prompt(new Prompt(new UserMessage("그거 어떻게 됐어요?")))
        .build();

    ChatClientResponse response = new RagAdvisor(vectorStore, 2, 0.62).adviseCall(request, chain);

    verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    assertThat(response.context())
        .containsEntry(RagAdvisor.RAG_ATTEMPTED, false)
        .containsEntry(RagAdvisor.RETRIEVED_DOCUMENTS, List.of());
  }

  @Test
  void HelpDesk_검색은_튜닝된_topK와_threshold를_사용한다() {
    VectorStore vectorStore = mock(VectorStore.class);
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
    CallAdvisorChain chain = mock(CallAdvisorChain.class);
    when(chain.nextCall(any())).thenReturn(ChatClientResponse.builder().build());
    ChatClientRequest request = ChatClientRequest.builder()
        .prompt(new Prompt(new UserMessage("반품 규정 알려줘")))
        .build();

    new RagAdvisor(vectorStore, 2, 0.29).adviseCall(request, chain);

    ArgumentCaptor<SearchRequest> search = ArgumentCaptor.forClass(SearchRequest.class);
    verify(vectorStore).similaritySearch(search.capture());
    assertThat(search.getValue().getTopK()).isEqualTo(2);
    assertThat(search.getValue().getSimilarityThreshold()).isEqualTo(0.29);
  }

  @Test
  void 대명사_후속질문은_최근_사용자_발화를_검색어에_포함한다() {
    VectorStore vectorStore = mock(VectorStore.class);
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
    CallAdvisorChain chain = mock(CallAdvisorChain.class);
    when(chain.nextCall(any())).thenReturn(ChatClientResponse.builder().build());
    ChatClientRequest request = ChatClientRequest.builder().prompt(new Prompt(List.of(
        new UserMessage("반품 규정 알려줘"),
        new AssistantMessage("7일 이내입니다."),
        new UserMessage("제 주문 12345는 어디예요?"),
        new AssistantMessage("배송 중입니다."),
        new UserMessage("그럼 그거 반품 돼요?")))).build();

    new RagAdvisor(vectorStore, 2, 0.29).adviseCall(request, chain);

    ArgumentCaptor<SearchRequest> search = ArgumentCaptor.forClass(SearchRequest.class);
    verify(vectorStore).similaritySearch(search.capture());
    assertThat(search.getValue().getQuery())
        .contains("반품 규정 알려줘", "제 주문 12345", "그럼 그거 반품 돼요?");
  }
}
