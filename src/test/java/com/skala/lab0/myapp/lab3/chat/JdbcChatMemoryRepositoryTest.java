package com.skala.lab0.myapp.lab3.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class JdbcChatMemoryRepositoryTest {
  @Autowired
  ChatMemory memory;

  @Autowired
  JdbcChatMemoryRepository repository;

  @Autowired
  JdbcTemplate jdbc;

  @Test
  void 최근_20개만_유지하고_사용자와_세션을_격리한다() {
    String conversation = ConversationIds.of("skala", "memory-user", "session-a");
    String otherSession = ConversationIds.of("skala", "memory-user", "session-b");
    String otherUser = ConversationIds.of("skala", "other-user", "session-a");
    memory.clear(conversation);
    memory.clear(otherSession);
    memory.clear(otherUser);

    for (int i = 0; i < 21; i++) memory.add(conversation, new UserMessage("message-" + i));

    assertThat(memory.get(conversation)).hasSize(20);
    assertThat(memory.get(conversation).getFirst().getText()).isEqualTo("message-1");
    assertThat(memory.get(otherSession)).isEmpty();
    assertThat(memory.get(otherUser)).isEmpty();
  }

  @Test
  void 새_저장소_인스턴스에서도_Tool_호출과_응답을_원본대로_복원한다() {
    String conversation = ConversationIds.of("skala", "memory-tool-user", "session-a");
    AssistantMessage assistant = AssistantMessage.builder().content("").toolCalls(List.of(
        new AssistantMessage.ToolCall("call-123", "function", "getOrder",
            "{\"orderId\":\"12345\"}"))).build();
    ToolResponseMessage tool = ToolResponseMessage.builder().responses(List.of(
        new ToolResponseMessage.ToolResponse("call-123", "getOrder", "SHIPPING"))).build();
    repository.saveAll(conversation, List.of(new UserMessage("주문 조회"), assistant, tool));

    List<Message> restored = new JdbcChatMemoryRepository(jdbc).findByConversationId(conversation);

    assertThat(restored).hasSize(3);
    AssistantMessage restoredAssistant = (AssistantMessage) restored.get(1);
    assertThat(restoredAssistant.getToolCalls()).singleElement().satisfies(call -> {
      assertThat(call.id()).isEqualTo("call-123");
      assertThat(call.name()).isEqualTo("getOrder");
      assertThat(call.arguments()).isEqualTo("{\"orderId\":\"12345\"}");
    });
    ToolResponseMessage restoredTool = (ToolResponseMessage) restored.get(2);
    assertThat(restoredTool.getResponses()).singleElement().satisfies(response -> {
      assertThat(response.id()).isEqualTo("call-123");
      assertThat(response.name()).isEqualTo("getOrder");
      assertThat(response.responseData()).isEqualTo("SHIPPING");
    });
  }

  @Test
  void 기존_JPA_형식의_일반_메시지도_복원한다() {
    String conversation = ConversationIds.of("skala", "legacy-user", "session-a");
    repository.deleteByConversationId(conversation);
    jdbc.update("insert into chat_messages (conversation_id, sequence_no, message_type, content) values (?, ?, ?, ?)",
        conversation, 0, "USER", "기존 대화");

    assertThat(repository.findByConversationId(conversation))
        .singleElement().satisfies(message -> assertThat(message.getText()).isEqualTo("기존 대화"));
  }
}
