package com.skala.helpdesk.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 2. 다른 패키지에 있는 클래스들을 명시적으로 import
import com.skala.helpdesk.rag.dto.RagAnswerDto;

@Slf4j
@SpringBootTest
class RagEvaluationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private AnswerService service; // 1. 타입명 변경

    record Golden(String q, List<String> must, String src){}

    @Test // 모델을 부르므로 기본 테스트에서는 제외한다 
    void 골든_세트_평가() throws Exception {

        InputStream resource = new ClassPathResource("golden.json").getInputStream();
        
        // 2. TypeReference 제네릭 문법 수정
        var golden = mapper.readValue(resource, new TypeReference<List<Golden>>() {});
        
        int pass = 0;
        for (Golden g: golden) {
            RagAnswerDto a = service.ask(g.q()); // 1. 타입명 변경
            
            boolean hit = g.must().stream().allMatch(k -> a.answer().contains(k));
            
            // 3. 마지막 괄호 추가하여 조건문 정상적으로 닫기
            boolean cite = g.src() == null
                        || (a.sources() != null && a.sources().stream().anyMatch(s -> s.contains(g.src())));
                        
            if (hit && cite) { 
                pass++;
            } else {
                log.warn("실패: {}\n  답변: {}\n  출처: {}", g.q(), a.answer(), a.sources());
            }
        }
        log.info("통과 {}/{}", pass, golden.size());
        assertThat(pass).isGreaterThanOrEqualTo(8);
    }
}