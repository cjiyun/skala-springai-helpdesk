package com.skala.lab0.myapp.lab3.web;

import java.security.Principal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skala.lab0.myapp.lab3.chat.Lab3ChatService;
import com.skala.lab0.myapp.lab3.chat.AnswerDto;
import com.skala.lab0.myapp.lab3.chat.HistoryDto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import com.skala.lab0.myapp.lab3.tools.ToolUsage;
import com.skala.lab0.myapp.lab3.advisor.RagAdvisor;
import com.skala.lab0.myapp.lab3.chat.Source;
import org.springframework.ai.document.Document;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.Disposable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;



@RestController
@RequestMapping("/api/chat")
public class Lab3ChatController {

        private final Lab3ChatService chatService;

        public Lab3ChatController(Lab3ChatService chatService) {
            this.chatService = chatService;
        }

        @PostMapping(value = "/stream", produces = "text/event-stream")
        public SseEmitter stream(@RequestBody ChatRequest request, Principal principal) {
            SseEmitter emitter = new SseEmitter(60_000L);
            ToolUsage usage = new ToolUsage();
            AtomicReference<List<Source>> sources = new AtomicReference<>(List.of());
            AtomicReference<Disposable> subscription = new AtomicReference<>();
            Disposable disposable = chatService.stream(principal.getName(), required(request.sessionId(), "sessionId"),
                    required(request.question(), "question"), usage)
                .subscribe(response -> {
                    try {
                        String token = response.chatResponse().getResult().getOutput().getText();
                        if (token != null && !token.isEmpty()) emitter.send(SseEmitter.event().name("token").data(token));
                        Object docs = response.context().get(RagAdvisor.RETRIEVED_DOCUMENTS);
                        if (docs instanceof List<?> list) sources.set(list.stream().filter(Document.class::isInstance)
                            .map(Document.class::cast).map(d -> new Source(
                                String.valueOf(d.getMetadata().getOrDefault("source", "unknown")),
                                String.valueOf(d.getMetadata().getOrDefault("version", "unknown"))))
                            .distinct().toList());
                    } catch (IOException error) { emitter.completeWithError(error); }
                }, emitter::completeWithError, () -> {
                    try { emitter.send(SseEmitter.event().name("sources").data(sources.get())); emitter.complete(); }
                    catch (IOException error) { emitter.completeWithError(error); }
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
            HistoryDto historyResponse = chatService.getHistory(principal.getName(), sessionId);
            return ResponseEntity.ok(historyResponse);
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
}
