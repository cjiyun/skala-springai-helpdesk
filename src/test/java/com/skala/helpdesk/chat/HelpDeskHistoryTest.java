package com.skala.helpdesk.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class HelpDeskHistoryTest {
  @Test
  void 대화_이력을_메시지와_Tool_상태로_구조화하고_세션을_삭제한다() {
    ChatMemory memory = mock(ChatMemory.class);
    String conversationId = "skala:user1:s1";
    AssistantMessage toolCall = AssistantMessage.builder().content("").toolCalls(List.of(
        new AssistantMessage.ToolCall("call-1", "function", "getOrder", "{\"orderId\":\"12345\"}")))
        .build();
    ToolResponseMessage toolResponse = ToolResponseMessage.builder().responses(List.of(
        new ToolResponseMessage.ToolResponse("call-1", "getOrder", "SHIPPING"))).build();
    when(memory.get(conversationId)).thenReturn(List.of(
        new UserMessage("내 주문 12345는?"), toolCall, toolResponse,
        new AssistantMessage("배송 중입니다.")));
    HelpDeskService service = new HelpDeskService(
        mock(ChatClient.class), memory, "fallback", "skala", new SimpleMeterRegistry());

    HistoryDto result = service.getHistory("user1", "s1");

    assertThat(result.history()).extracting(HistoryDto.HistoryMessage::role)
        .containsExactly("USER", "TOOL", "TOOL", "ASSISTANT");
    assertThat(result.history().get(1).toolName()).isEqualTo("getOrder");
    assertThat(result.history().get(1).status()).isEqualTo("호출");
    assertThat(result.history().get(2).status()).isEqualTo("완료");
    assertThat(result.history()).noneMatch(message ->
        message.role().equals("ASSISTANT") && (message.content() == null || message.content().isBlank()));

    service.clearHistory("user1", "s1");
    verify(memory).clear(conversationId);
  }
}
