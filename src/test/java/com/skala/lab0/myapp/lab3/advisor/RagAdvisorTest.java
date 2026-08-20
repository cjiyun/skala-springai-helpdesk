package com.skala.lab0.myapp.lab3.advisor;

import com.skala.lab0.myapp.lab3.chat.Lab3ChatResponse;
import com.skala.lab0.myapp.lab3.chat.Lab3ChatService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class RagAdvisorTest {

    @Autowired
    private Lab3ChatService chatService;

    @Test
    @DisplayName("규정_질문에_문서_검색_기반_답변과_출처가_반환된다")
    void 규정_질문_RAG_답변_검증() {
        String userId = "user1";
        String sessionId = "session-rag-test";
        String question = "단순 변심 반품은 며칠 이내인가요?";

        Lab3ChatResponse response = chatService.chat(userId, sessionId, question);

        log.info("[RAG 검증] 질문: {}", question);
        log.info("[RAG 검증] 답변: {}", response.answer());

        // 1. 규정 핵심 키워드 및 출처 텍스트 포함 검증
        assertThat(response.answer())
                .contains("7일")
                .contains("return-policy.md");
    }
}