package com.skala.lab0.myapp.lab3.chat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.openai.OpenAiChatOptions;
import com.skala.lab0.myapp.lab3.advisor.RagAdvisor;
import org.springframework.ai.document.Document;
import com.skala.lab0.myapp.lab3.tools.ToolUsage;
import reactor.core.publisher.Flux;
import com.openai.errors.OpenAIException;

@Service
public class Lab3ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final String fallbackModel;
    private final String tenantId;

    public Lab3ChatService(ChatClient assistantChatClient, ChatMemory chatMemory,
            @Value("${helpdesk.model.fallback:gpt-4o-mini}") String fallbackModel,
            @Value("${helpdesk.tenant-id:skala}") String tenantId) {
        this.chatClient = assistantChatClient;
        this.chatMemory = chatMemory;
        this.fallbackModel = fallbackModel;
        this.tenantId = tenantId;
    }
    /**
     * 챗봇 상담 처리
     * - userId와 sessionId를 조합해 세션별 대화 문맥 격리
     * - ChatClient Advisor 체인 실행
     */
    public AnswerDto chat(String userId, String sessionId, String message) {
        String conversationId = ConversationIds.of(tenantId, userId, sessionId);
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);

        try {
            ChatClientResponse response;
            ToolUsage usage = new ToolUsage();
            try {
                response = call(message, conversationId, userId, null, usage);
            } catch (OpenAIException primaryFailure) {
                if (usage.wasWriteUsed()) throw primaryFailure;
                response = call(message, conversationId, userId, fallbackModel, usage);
            }

            String botReply = response.chatResponse().getResult().getOutput().getText();
            @SuppressWarnings("unchecked")
            List<Document> documents = (List<Document>) response.context()
                    .getOrDefault(RagAdvisor.RETRIEVED_DOCUMENTS, List.of());
            List<Source> sources = documents.stream()
                    .map(document -> new Source(
                            String.valueOf(document.getMetadata().getOrDefault("source", "unknown")),
                            String.valueOf(document.getMetadata().getOrDefault("version", "unknown"))))
                    .distinct().toList();
            boolean ragAttempted = Boolean.TRUE.equals(response.context().get(RagAdvisor.RAG_ATTEMPTED));
            if (ragAttempted && sources.isEmpty()) {
                return new AnswerDto("관련 사내 규정 근거가 확인되지 않습니다.", List.of(), usage.wasUsed());
            }
            return new AnswerDto(botReply, sources, usage.wasUsed());
        } finally {
            MDC.remove("traceId");
        }
    }

    private ChatClientResponse call(String message, String conversationId, String userId, String model, ToolUsage usage) {
        var request = chatClient.prompt().user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of("userId", userId, ToolUsage.CONTEXT_KEY, usage));
        if (model != null) request.options(OpenAiChatOptions.builder().model(model));
        return request.call().chatClientResponse();
    }

    public Flux<ChatClientResponse> stream(String userId, String sessionId, String message, ToolUsage usage) {
        String conversationId = ConversationIds.of(tenantId, userId, sessionId);
        return streamCall(message, conversationId, userId, null, usage)
                .onErrorResume(OpenAIException.class,
                        error -> usage.wasWriteUsed() ? Flux.error(error)
                                : streamCall(message, conversationId, userId, fallbackModel, usage));
    }

    private Flux<ChatClientResponse> streamCall(String message, String conversationId, String userId,
            String model, ToolUsage usage) {
        var request = chatClient.prompt().user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, conversationId))
                .toolContext(Map.of("userId", userId, ToolUsage.CONTEXT_KEY, usage));
        if (model != null) request.options(OpenAiChatOptions.builder().model(model));
        return request.stream().chatClientResponse();
    }
    /**
     * 특정 사용자 세션의 대화 이력 조회
     */
    public HistoryDto getHistory(String userId, String sessionId) {
        String conversationId = ConversationIds.of(tenantId, userId, sessionId);
        List<Message> history = chatMemory.get(conversationId);

        List<String> messageTexts = history.stream()
                .map(msg -> msg.getMessageType().name() + ": " + msg.getText())
                .toList();

        return new HistoryDto(messageTexts);
    }
}
