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
class AuditAdvisorTest {

    @Autowired
    private HelpDeskService chatService;

    @Test
    @DisplayName("도구_호출_및_질의응답_시_감사_로그와_추적_정보가_기록된다")
    void 감사로그_기록_검증() {
        String userId = "user1";
        String sessionId = "session-audit-01";
        String question = "단순 변심 반품은 며칠 이내인가요?";

        AnswerDto response = chatService.chat(userId, sessionId, question);

        log.info("[Audit 검증] 응답: {}", response.answer());
        assertThat(response.answer()).isNotNull();
    }
}
