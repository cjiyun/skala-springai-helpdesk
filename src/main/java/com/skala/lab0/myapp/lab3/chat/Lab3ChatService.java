package com.skala.lab0.myapp.lab3.chat;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import com.skala.lab0.myapp.rag.dto.Lab2AnswerDto;

@Service
public class Lab3ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public Lab3ChatService (ChatClient assistantChatClient, ChatMemory chatMemory) {
        this.chatClient = assistantChatClient;
        this.chatMemory = chatMemory;
    }
    /**
     * 챗봇 상담 처리
     * - userId와 sessionId를 조합해 세션별 대화 문맥 격리
     * - ChatClient Advisor 체인 실행
     */
    public Lab3ChatResponse chat(String userId, String sessionId, String message) {
        String conversationId = userId + ":" + sessionId;

        String botReply = chatClient.prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param("chat_memory_conversation_id", conversationId))
                .call()
                .content();

        return Lab3ChatResponse.of(botReply);
    }
    /**
     * 특정 사용자 세션의 대화 이력 조회
     */
    public Lab3ChatResponse getHistory(String userId, String sessionId) {
        String conversationId = userId + ":" + sessionId;
        List<Message> history = chatMemory.get(conversationId);

        List<String> messageTexts = history.stream()
                .map(msg -> msg.getMessageType().name() + ": " + msg.getText())
                .toList();

        return Lab3ChatResponse.ofHistory(messageTexts);
    }

    /**
     * Day 2 RAG 결과를 Day 3 응답 포맷으로 변환
     */
    public Lab3ChatResponse convertRagResponse(Lab2AnswerDto ragDto) {
        return Lab3ChatResponse.fromRag(ragDto);
    }
}