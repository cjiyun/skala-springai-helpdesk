package com.skala.helpdesk.chat;

import java.util.List;

import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class JdbcChatMemoryRepository implements ChatMemoryRepository {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json = new ObjectMapper();

  public JdbcChatMemoryRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<String> findConversationIds() {
    return jdbc.queryForList("select distinct conversation_id from chat_messages", String.class);
  }

  @Override
  public List<Message> findByConversationId(String conversationId) {
    return jdbc.query("select message_type, content from chat_messages where conversation_id = ? order by sequence_no",
        (result, row) -> decode(result.getString("message_type"), result.getString("content")), conversationId);
  }

  @Override
  @Transactional
  public void saveAll(String conversationId, List<Message> history) {
    deleteByConversationId(conversationId);
    for (int i = 0; i < history.size(); i++) {
      Message message = history.get(i);
      jdbc.update("insert into chat_messages (conversation_id, sequence_no, message_type, content) values (?, ?, ?, ?)",
          conversationId, i, message.getMessageType().name(), encode(message));
    }
  }

  @Override
  public void deleteByConversationId(String conversationId) {
    jdbc.update("delete from chat_messages where conversation_id = ?", conversationId);
  }

  private String encode(Message message) {
    List<StoredToolCall> calls = message instanceof AssistantMessage assistant
        ? assistant.getToolCalls().stream().map(call ->
            new StoredToolCall(call.id(), call.type(), call.name(), call.arguments())).toList()
        : List.of();
    List<StoredToolResponse> responses = message instanceof ToolResponseMessage tool
        ? tool.getResponses().stream().map(response ->
            new StoredToolResponse(response.id(), response.name(), response.responseData())).toList()
        : List.of();
    try {
      return json.writeValueAsString(new StoredMessage(
          message.getMessageType().name(), message.getText(), calls, responses));
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize chat message", exception);
    }
  }

  private Message decode(String type, String content) {
    try {
      StoredMessage stored = json.readValue(content, StoredMessage.class);
      return switch (stored.type()) {
        case "USER" -> new UserMessage(stored.text());
        case "SYSTEM" -> new SystemMessage(stored.text());
        case "TOOL" -> ToolResponseMessage.builder().responses(stored.responses().stream()
            .map(response -> new ToolResponseMessage.ToolResponse(
                response.id(), response.name(), response.data())).toList()).build();
        default -> AssistantMessage.builder().content(stored.text()).toolCalls(stored.calls().stream()
            .map(call -> new AssistantMessage.ToolCall(
                call.id(), call.type(), call.name(), call.arguments())).toList()).build();
      };
    } catch (JsonProcessingException exception) {
      return switch (type) {
        case "USER" -> new UserMessage(content);
        case "SYSTEM" -> new SystemMessage(content);
        case "TOOL" -> ToolResponseMessage.builder().responses(List.of(
            new ToolResponseMessage.ToolResponse("legacy", "legacyTool", content))).build();
        default -> new AssistantMessage(content);
      };
    }
  }

  private record StoredMessage(String type, String text, List<StoredToolCall> calls,
      List<StoredToolResponse> responses) {}
  private record StoredToolCall(String id, String type, String name, String arguments) {}
  private record StoredToolResponse(String id, String name, String data) {}
}
