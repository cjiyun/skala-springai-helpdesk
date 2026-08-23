package com.skala.helpdesk;

import com.skala.helpdesk.chat.AnswerDto;
import com.skala.helpdesk.chat.HelpDeskService;
import com.skala.helpdesk.service.TicketService;
import java.util.UUID;
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
class MultiTurnScenarioTest {

    @Autowired
    private HelpDeskService chatService;

    @Autowired
    private TicketService ticketService;

    // 세션 A (1~4턴 대화 맥락 유지)
    private static final String USER_ID = "user1";
    private static final String RUN_ID = UUID.randomUUID().toString();
    private static final String SESSION_A = "session-multiturn-report-01-" + RUN_ID;
    // 세션 B (5턴 새 세션 격리 확인)
    private static final String SESSION_B = "session-multiturn-report-02-" + RUN_ID;

    @Test
    @Order(1)
    @DisplayName("Turn 1. RAG — 규정 답변 + 출처")
    void turn1_ragPolicy() {
        String q = "단순 변심 반품은 며칠 이내인가요?";
        AnswerDto res = chatService.chat(USER_ID, SESSION_A, q);
        
        log.info("[Turn 1] Q: {}", q);
        log.info("[Turn 1] A: {}", res.answer());

        assertThat(res.answer()).containsAnyOf("7일", "반품");
        assertThat(res.sources()).anySatisfy(source -> {
            assertThat(source.document()).contains("return-policy.md");
            assertThat(source.version()).isNotBlank();
        });
        assertThat(res.toolUsed()).isFalse();
    }

    @Test
    @Order(2)
    @DisplayName("Turn 2. 도구 — 실시간 상태 조회")
    void turn2_toolOrderCheck() {
        String q = "제 주문 12345는 지금 어디예요?";
        AnswerDto res = chatService.chat(USER_ID, SESSION_A, q);

        log.info("[Turn 2] Q: {}", q);
        log.info("[Turn 2] A: {}", res.answer());

        assertThat(res.answer())
                .contains("12345")
                .containsAnyOf("무선 이어폰", "배송 중", "2026-08-20");
        assertThat(res.toolUsed()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Turn 3. 메모리 — 1·2를 함께 참조(대명사 해석)")
    void turn3_multiTurnMemory() {
        String q = "그럼 그거 반품 돼요?";
        AnswerDto res = chatService.chat(USER_ID, SESSION_A, q);

        log.info("[Turn 3] Q: {}", q);
        log.info("[Turn 3] A: {}", res.answer());

        assertThat(res.answer())
                .containsAnyOf("7일", "반품")
                .containsAnyOf("12345", "무선 이어폰");
    }

    @Test
    @Order(4)
    @DisplayName("Turn 4. 교환 승인 게이트 — 티켓 번호 + 대기 안내")
    void turn4_exchangeApprovalGate() {
        String q = "교환으로 접수해 주세요";
        AnswerDto res = chatService.chat(USER_ID, SESSION_A, q);

        log.info("[Turn 4] Q: {}", q);
        log.info("[Turn 4] A: {}", res.answer());

        var ticket = ticketService.pending().stream()
                .filter(candidate -> candidate.orderId().equals("12345"))
                .filter(candidate -> candidate.type().equals("EXCHANGE"))
                .findFirst()
                .orElseThrow();
        assertThat(ticket.status()).isEqualTo("PENDING");
        assertThat(ticket.type()).isEqualTo("EXCHANGE");
        assertThat(res.toolUsed()).isTrue();
        assertThat(res.answer())
                .contains(ticket.ticketNo())
                .containsAnyOf("접수", "대기", "PENDING", "승인", "티켓");
    }

    @Test
    @Order(5)
    @DisplayName("Turn 5. 티켓 조회 — 이전 교환 접수 상태 확인")
    void turn5_ticketStatus() {
        String q = "그 교환 접수는 어떻게 됐어요?";
        AnswerDto res = chatService.chat(USER_ID, SESSION_A, q);

        assertThat(res.answer()).containsAnyOf("PENDING", "승인 대기", "대기 중");
        assertThat(res.toolUsed()).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("Turn 6. 맥락 없음 — 되묻는다(세션 격리)")
    void turn6_sessionIsolation() {
        String q = "그거 어떻게 됐어요?";
        AnswerDto res = chatService.chat(USER_ID, SESSION_B, q);

        log.info("[Turn 5] Q: {}", q);
        log.info("[Turn 5] A: {}", res.answer());

        // 이전 세션 A의 주문번호나 진행 내역을 모른 채 되물어야 함
        assertThat(res.answer())
                .doesNotContain("12345", "search-experiments", "A 옵션", "우주 배송")
                .containsAnyOf("무엇", "어떤", "주문번호", "말씀");
    }

    @Test
    @Order(7)
    @DisplayName("Turn 7. 다른 사용자에게 기존 세션 맥락이 노출되지 않는다")
    void turn7_userIsolation() {
        AnswerDto res = chatService.chat("user2", SESSION_A, "그 주문은 어떻게 됐어요?");

        assertThat(res.answer())
                .doesNotContain("12345", "무선 이어폰")
                .containsAnyOf("무엇", "어떤", "주문번호", "말씀");
    }
}
