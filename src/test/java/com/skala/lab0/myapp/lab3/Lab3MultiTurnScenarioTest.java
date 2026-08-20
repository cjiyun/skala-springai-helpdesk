package com.skala.lab0.myapp.lab3;

import com.skala.lab0.myapp.lab3.chat.Lab3ChatResponse;
import com.skala.lab0.myapp.lab3.chat.Lab3ChatService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Lab3MultiTurnScenarioTest {

    @Autowired
    private Lab3ChatService chatService;

    // 세션 A (1~4턴 대화 맥락 유지)
    private static final String USER_ID = "user1";
    private static final String SESSION_A = "session-multiturn-report-01";
    // 세션 B (5턴 새 세션 격리 확인)
    private static final String SESSION_B = "session-multiturn-report-02";

    @Test
    @Order(1)
    @DisplayName("Turn 1. RAG — 규정 답변 + 출처")
    void turn1_ragPolicy() {
        String q = "단순 변심 반품은 며칠 이내인가요?";
        Lab3ChatResponse res = chatService.chat(USER_ID, SESSION_A, q);
        
        log.info("[Turn 1] Q: {}", q);
        log.info("[Turn 1] A: {}", res.answer());

        assertThat(res.answer()).containsAnyOf("7일", "반품");
        assertThat(res.answer()).contains("출처:");
    }

    @Test
    @Order(2)
    @DisplayName("Turn 2. 도구 — 실시간 상태 조회")
    void turn2_toolOrderCheck() {
        String q = "제 주문 12345는 지금 어디예요?";
        Lab3ChatResponse res = chatService.chat(USER_ID, SESSION_A, q);

        log.info("[Turn 2] Q: {}", q);
        log.info("[Turn 2] A: {}", res.answer());

        assertThat(res.answer()).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("Turn 3. 메모리 — 1·2를 함께 참조(대명사 해석)")
    void turn3_multiTurnMemory() {
        String q = "그럼 그거 반품 돼요?";
        Lab3ChatResponse res = chatService.chat(USER_ID, SESSION_A, q);

        log.info("[Turn 3] Q: {}", q);
        log.info("[Turn 3] A: {}", res.answer());

        assertThat(res.answer()).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("Turn 4. 승인 게이트 — 티켓 번호 + 대기 안내")
    void turn4_refundApprovalGate() {
        String q = "환불로 접수해 주세요";
        Lab3ChatResponse res = chatService.chat(USER_ID, SESSION_A, q);

        log.info("[Turn 4] Q: {}", q);
        log.info("[Turn 4] A: {}", res.answer());

        assertThat(res.answer()).containsAnyOf("접수", "대기", "PENDING", "승인", "티켓");
    }

    @Test
    @Order(5)
    @DisplayName("Turn 5. 맥락 없음 — 되묻는다(세션 격리)")
    void turn5_sessionIsolation() {
        String q = "그거 어떻게 됐어요?";
        Lab3ChatResponse res = chatService.chat(USER_ID, SESSION_B, q);

        log.info("[Turn 5] Q: {}", q);
        log.info("[Turn 5] A: {}", res.answer());

        // 이전 세션 A의 주문번호나 진행 내역을 모른 채 되물어야 함
        assertThat(res.answer()).doesNotContain("12345");
    }
}