package com.skala.helpdesk.chat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.openai.OpenAiChatOptions;
import com.skala.helpdesk.advisor.RagAdvisor;
import org.springframework.ai.document.Document;
import com.skala.helpdesk.tools.ToolUsage;
import reactor.core.publisher.Flux;
import com.openai.errors.OpenAIException;
import com.openai.errors.InternalServerException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.RateLimitException;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class HelpDeskService {

    public static final String NO_EVIDENCE_MARKER = "NO_EVIDENCE";
    public static final String NO_EVIDENCE_REPLY = "관련 사내 규정 근거가 확인되지 않습니다.";

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final String fallbackModel;
    private final String tenantId;
    private final MeterRegistry meterRegistry;

    public HelpDeskService(ChatClient assistantChatClient, ChatMemory chatMemory,
            @Value("${helpdesk.model.fallback:gpt-4o-mini}") String fallbackModel,
            @Value("${helpdesk.tenant-id:skala}") String tenantId,
            MeterRegistry meterRegistry) {
        this.chatClient = assistantChatClient;
        this.chatMemory = chatMemory;
        this.fallbackModel = fallbackModel;
        this.tenantId = tenantId;
        this.meterRegistry = meterRegistry;
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
                if (usage.wasWriteUsed() || !retryable(primaryFailure)) throw primaryFailure;
                fallbackAttempt("chat");
                try {
                    response = call(message, conversationId, userId, fallbackModel, usage);
                    fallbackOutcome("chat", "success");
                } catch (RuntimeException fallbackFailure) {
                    fallbackOutcome("chat", "error");
                    throw fallbackFailure;
                }
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
            if (ragAttempted && (sources.isEmpty() || saysNoEvidence(botReply))) {
                return new AnswerDto(NO_EVIDENCE_REPLY, List.of(), usage.wasUsed());
            }
            return new AnswerDto(botReply, sources, usage.wasUsed());
        } finally {
            MDC.remove("traceId");
        }
    }

    private boolean saysNoEvidence(String answer) {
        return answer.contains(NO_EVIDENCE_MARKER) || answer.contains("근거가 없어")
                || answer.contains("확인되지 않습니다") || answer.contains("확인할 수 없습니다")
                || answer.contains("확인된 정보를 드릴 수 없습니다");
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
        AtomicBoolean emitted = new AtomicBoolean();
        return Flux.defer(() -> streamCall(message, conversationId, userId, null, usage))
                .doOnNext(response -> emitted.set(true))
                .onErrorResume(error -> error instanceof OpenAIException openAiError
                                && !emitted.get() && !usage.wasWriteUsed() && retryable(openAiError),
                        error -> fallbackStream(message, conversationId, userId, usage));
    }

    private Flux<ChatClientResponse> fallbackStream(String message, String conversationId,
            String userId, ToolUsage usage) {
        fallbackAttempt("chat-stream");
        try {
            return streamCall(message, conversationId, userId, fallbackModel, usage)
                    .doOnComplete(() -> fallbackOutcome("chat-stream", "success"))
                    .doOnError(error -> fallbackOutcome("chat-stream", "error"));
        } catch (RuntimeException fallbackFailure) {
            fallbackOutcome("chat-stream", "error");
            return Flux.error(fallbackFailure);
        }
    }

    private void fallbackAttempt(String feature) {
        meterRegistry.counter("ai.fallback.calls", "feature", feature).increment();
    }

    private void fallbackOutcome(String feature, String result) {
        meterRegistry.counter("ai.fallback.outcomes", "feature", feature, "result", result).increment();
    }

    private boolean retryable(OpenAIException error) {
        return error instanceof OpenAIIoException
                || error instanceof InternalServerException
                || error instanceof RateLimitException;
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
