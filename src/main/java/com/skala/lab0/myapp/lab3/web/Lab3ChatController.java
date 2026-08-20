package com.skala.lab0.myapp.lab3.web;

import java.security.Principal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skala.lab0.myapp.lab3.chat.Lab3ChatResponse;
import com.skala.lab0.myapp.lab3.chat.Lab3ChatService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/lab3/chat")
public class Lab3ChatController {

        private final Lab3ChatService chatService;

        public Lab3ChatController(Lab3ChatService chatService) {
            this.chatService = chatService;
        }

        public record ChatRequest(String sessionId, String message) {}

        /**
         * POST /lab3/chat
         * AI 어시스턴트 상담 API
         */
        @PostMapping
        public ResponseEntity<Lab3ChatResponse> chat(
            @RequestBody ChatRequest request,
            Principal principal) {
            Lab3ChatResponse response = chatService.chat(
                principal.getName(),
                request.sessionId(),
                request.message()
            );
            return ResponseEntity.ok(response);
        }

        /**
         * GET /lab3/chat/history?sessionId=yyy
         * 대화 이력 조회 API
         */
        @GetMapping("/history")
        public ResponseEntity<Lab3ChatResponse> getHistoryEntity(
            @RequestParam String sessionId,
            Principal principal
        ) {
            Lab3ChatResponse historyResponse = chatService.getHistory(principal.getName(), sessionId);
            return ResponseEntity.ok(historyResponse);
        }
}
