package com.skala.lab0.myapp.lab3.advisor;

import com.skala.lab0.myapp.lab3.chat.Lab3ChatResponse;
import com.skala.lab0.myapp.lab3.chat.Lab3ChatService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class MetricsAdvisorTest {

    @Autowired
    private Lab3ChatService chatService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    @DisplayName("질의응답_수행_후_Actuator_메트릭_지표가_정상적으로_누적된다")
    void 계측_메트릭_수집_검증() {
        String userId = "user1";
        String sessionId = "session-metrics-01";
        String question = "제 주문 12345는 지금 어디예요?";
        Counter counterBefore = meterRegistry.find("ai.tool.calls")
                .tag("tool", "getOrder")
                .tag("result", "ok")
                .counter();
        double toolCallsBefore = counterBefore == null ? 0 : counterBefore.count();

        // 1. 질의응답 호출
        Lab3ChatResponse res = chatService.chat(userId, sessionId, question);
        assertThat(res.answer()).isNotNull();

        // 2. 누적된 메트릭 확인 및 로그 출력
        log.info("========== [Actuator / Micrometer 메트릭 수집 현황] ==========");
        meterRegistry.getMeters().stream()
                .filter(meter -> meter.getId().getName().contains("gen_ai") ||
                                 meter.getId().getName().contains("spring.ai") ||
                                 meter.getId().getName().contains("chat") ||
                                 meter.getId().getName().contains("timer") ||
                                 meter.getId().getName().contains("http"))
                .limit(10)
                .forEach(meter -> log.info("Metric Registered: {} | Type: {}", 
                        meter.getId().getName(), meter.getId().getType()));

        assertThat(meterRegistry.find("ai.tokens").counters()).isNotEmpty();
        assertThat(meterRegistry.find("ai.latency").timers()).isNotEmpty();
        Counter counterAfter = meterRegistry.find("ai.tool.calls")
                .tag("tool", "getOrder")
                .tag("result", "ok")
                .counter();
        assertThat(counterAfter).isNotNull();
        assertThat(counterAfter.count()).isGreaterThan(toolCallsBefore);
    }
}
