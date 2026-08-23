package com.skala.helpdesk.web;

import java.security.Principal;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skala.helpdesk.chat.HelpDeskService;
import com.skala.helpdesk.chat.AnswerDto;
import com.skala.helpdesk.chat.HistoryDto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import com.skala.helpdesk.tools.ToolUsage;
import com.skala.helpdesk.advisor.RagAdvisor;
import com.skala.helpdesk.chat.Source;
import org.springframework.ai.document.Document;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.Disposable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;



@RestController
@RequestMapping("/api/chat")
public class ChatController {

        private static final Logger log = LoggerFactory.getLogger(ChatController.class);
        private static final ObjectMapper JSON = new ObjectMapper();
        private final HelpDeskService chatService;

        public ChatController(HelpDeskService chatService) {
            this.chatService = chatService;
        }

        @PostMapping(value = "/stream", produces = "text/event-stream")
        public SseEmitter stream(@RequestBody ChatRequest request, Principal principal) {
            SseEmitter emitter = new SseEmitter(60_000L);
            ToolUsage usage = new ToolUsage();
            StreamState state = new StreamState();
            AtomicReference<Disposable> subscription = new AtomicReference<>();
            Disposable disposable = chatService.stream(principal.getName(), required(request.sessionId(), "sessionId"),
                    required(request.question(), "question"), usage)
                .subscribe(response -> {
                    try {
                        state.updateContext(response.context());
                        String token = null;
                        if (response.chatResponse() != null
                                && response.chatResponse().getResult() != null
                                && response.chatResponse().getResult().getOutput() != null) {
                            token = response.chatResponse().getResult().getOutput().getText();
                        }
                        state.sendToken(token, emitter);
                    } catch (IOException error) {
                        log.warn("SSE token 전송 실패", error);
                        emitter.complete();
                    }
                }, error -> {
                    log.warn("SSE 모델 스트림 실패", error);
                    emitter.complete();
                }, () -> {
                    try {
                        state.complete(emitter);
                        emitter.complete();
                    }
                    catch (IOException error) {
                        log.warn("SSE sources 전송 실패", error);
                        emitter.complete();
                    }
                });
            subscription.set(disposable);
            emitter.onTimeout(() -> dispose(subscription));
            emitter.onCompletion(() -> dispose(subscription));
            emitter.onError(error -> dispose(subscription));
            return emitter;
        }

        public record ChatRequest(String sessionId, String question) {}

        /**
         * POST /api/chat
         * AI 어시스턴트 상담 API
         */
        @PostMapping
        public ResponseEntity<AnswerDto> chat(
            @RequestBody ChatRequest request,
            Principal principal) {
            AnswerDto response = chatService.chat(
                principal.getName(),
                required(request.sessionId(), "sessionId"),
                required(request.question(), "question")
            );
            return ResponseEntity.ok(response);
        }

        /**
         * GET /api/chat/history?sessionId=yyy
         * 대화 이력 조회 API
         */
        @GetMapping("/history")
        public ResponseEntity<HistoryDto> getHistoryEntity(
            @RequestParam String sessionId,
            Principal principal
        ) {
            HistoryDto historyResponse = chatService.getHistory(
                principal.getName(), required(sessionId, "sessionId"));
            return ResponseEntity.ok(historyResponse);
        }

        @DeleteMapping("/history")
        public ResponseEntity<Void> clearHistory(
            @RequestParam String sessionId,
            Principal principal
        ) {
            chatService.clearHistory(principal.getName(), required(sessionId, "sessionId"));
            return ResponseEntity.noContent().build();
        }

        private String required(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, name + " must not be blank");
            }
            return value.trim();
        }

        private void dispose(AtomicReference<Disposable> subscription) {
            Disposable disposable = subscription.get();
            if (disposable != null && !disposable.isDisposed()) disposable.dispose();
        }

        private static final class StreamState {
            private final StringBuilder pending = new StringBuilder();
            private List<Source> sources = List.of();
            private boolean ragAttempted;
            private boolean noEvidence;
            private boolean tokenSent;

            void updateContext(Map<String, Object> context) {
                ragAttempted |= Boolean.TRUE.equals(context.get(RagAdvisor.RAG_ATTEMPTED));
                Object docs = context.get(RagAdvisor.RETRIEVED_DOCUMENTS);
                if (docs instanceof List<?> list) {
                    sources = list.stream().filter(Document.class::isInstance)
                        .map(Document.class::cast).map(d -> new Source(
                            String.valueOf(d.getMetadata().getOrDefault("source", "unknown")),
                            String.valueOf(d.getMetadata().getOrDefault("version", "unknown"))))
                        .distinct().toList();
                    noEvidence |= ragAttempted && sources.isEmpty();
                }
            }

            void sendToken(String token, SseEmitter emitter) throws IOException {
                if (token == null || token.isEmpty() || noEvidence) return;
                pending.append(token);
                String buffered = pending.toString();
                int markerAt = buffered.indexOf(HelpDeskService.NO_EVIDENCE_MARKER);
                if (markerAt >= 0) {
                    if (markerAt > 0) send(emitter, buffered.substring(0, markerAt));
                    noEvidence = true;
                    pending.setLength(0);
                    return;
                }

                int retained = markerPrefixSuffixLength(buffered);
                int safeLength = buffered.length() - retained;
                if (safeLength > 0) send(emitter, buffered.substring(0, safeLength));
                pending.delete(0, safeLength);
            }

            void complete(SseEmitter emitter) throws IOException {
                noEvidence |= ragAttempted && sources.isEmpty();
                if (noEvidence) {
                    if (!tokenSent) send(emitter, HelpDeskService.NO_EVIDENCE_REPLY);
                    sources = List.of();
                } else if (!pending.isEmpty()) {
                    send(emitter, pending.toString());
                }
                emitter.send(SseEmitter.event().name("sources")
                    .data(JSON.writeValueAsString(sources), MediaType.TEXT_PLAIN));
            }

            private void send(SseEmitter emitter, String token) throws IOException {
                if (token.isEmpty()) return;
                emitter.send(SseEmitter.event().name("token").data(token));
                tokenSent = true;
            }

            private int markerPrefixSuffixLength(String text) {
                String marker = HelpDeskService.NO_EVIDENCE_MARKER;
                int limit = Math.min(text.length(), marker.length() - 1);
                for (int length = limit; length > 0; length--) {
                    if (text.regionMatches(text.length() - length, marker, 0, length)) return length;
                }
                return 0;
            }
        }
}
