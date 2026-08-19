package com.skala.lab0.myapp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;

// assertThat을 사용하기 위한 정적 임포트
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class RagEvaluationTest {
    
    @Autowired
    private ObjectMapper mapper;
    
    @Autowired
    private AnswerService service;

    record Golden(String q, List<String> must, String src){}

    @Test // 모델을 부르므로 기본 테스트에서는 제외한다 
    void 골든_세트_평가() throws Exception {

        InputStream resource = new ClassPathResource("golden.json").getInputStream();
        
        var golden = mapper.readValue(resource, new TypeReference(<List<Golden>>(){});
        int pass = 0;
        for (Golden g: golden) {
            AnswerDto a = service.ask(g.q());
            boolean hit = g.must().stream().allMatch(k -> a.answer().contains(k));
            boolean cite = g.src() == null
                        || (a.sources()!=null && a.sources().stream().anyMatch(s -> s.contains(g.src()));
            if (hit && cite) { pass ++;}
            else {log.warn("실패: {}\n  답변: {}\n  출처: {}", g.q(), a.answer(), a.sources());}
        }
        log.info("통과 {}/{}", pass, golden.size());
        assertThat(pass).isGreaterThanOrEqualTo(8);
    }
}
