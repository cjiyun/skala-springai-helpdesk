package com.skala.helpdesk.advisor;

import com.skala.helpdesk.chat.AnswerDto;
import com.skala.helpdesk.chat.HelpDeskService;
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
    private HelpDeskService chatService;

    @Test
    @DisplayName("규정_질문에_문서_검색_기반_답변과_출처가_반환된다")
    void 규정_질문_RAG_답변_검증() {
        String userId = "user1";
        String sessionId = "session-rag-test";
        String question = "단순 변심 반품은 며칠 이내인가요?";

        AnswerDto response = chatService.chat(userId, sessionId, question);

        log.info("[RAG 검증] 질문: {}", question);
        log.info("[RAG 검증] 답변: {}", response.answer());

        assertThat(response.answer()).contains("7일");
        assertThat(response.sources()).anySatisfy(source -> {
            assertThat(source.document()).isEqualTo("return-policy.md");
            assertThat(source.version()).isNotBlank();
        });
    }

    @Test
    @DisplayName("검색_근거가_없으면_추측하지_않는다")
    void 근거_없음_검증() {
        AnswerDto response = chatService.chat("user1", "session-no-evidence",
                "화성 탐사 우주복 지급 규정을 알려줘");

        assertThat(response.answer()).isEqualTo("관련 사내 규정 근거가 확인되지 않습니다.");
        assertThat(response.sources()).isEmpty();
    }
}
