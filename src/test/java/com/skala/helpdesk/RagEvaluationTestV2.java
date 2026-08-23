package com.skala.helpdesk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import com.skala.helpdesk.rag.dto.RagAnswerDto;
import com.skala.helpdesk.rag.AnswerService;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RagEvaluationTestV2 {

    private static final Logger log = LoggerFactory.getLogger(RagEvaluationTestV2.class);

    @Autowired
    private AnswerService answerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // JSON 맵핑을 위한 내부 레코드
    record GoldenItem(String q, List<String> must, String src) {}

    @Test
    void 골든_세트_평가() throws Exception {
        // 1. golden.json 파일 로드 (src/test/resources/golden.json 위치 기준)
        InputStream is = new ClassPathResource("golden.json").getInputStream();
        List<GoldenItem> goldenSet = objectMapper.readValue(is, new TypeReference<>() {});

        int passCount = 0;

        // 2. 골든 세트 문항별 평가 수행
        for (GoldenItem item : goldenSet) {
            RagAnswerDto result = answerService.ask(item.q());
            String answerText = result.answer();

            // must 키워드가 답변에 모두 포함되어 있는지 검증
            boolean passed = true;
            for (String keyword : item.must()) {
                if (!answerText.contains(keyword)) {
                    passed = false;
                    break;
                }
            }

            // 결과 로깅
            if (passed) {
                passCount++;
            } else {
                log.warn("실패: {}\n  답변: {}\n  출처: {}", item.q(), answerText, result.sources());
            }
        }

        log.info("통과 {}/{}", passCount, goldenSet.size());

        // 3. 최종 완료 기준 (8개 이상 통과) 검증
        assertThat(passCount).isGreaterThanOrEqualTo(8);
    }
}