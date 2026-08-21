package com.skala.lab0.myapp.lab3.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.skala.lab0.myapp.lab3.advisor.AuditAdvisor;
import com.skala.lab0.myapp.lab3.advisor.RagAdvisor;
import com.skala.lab0.myapp.lab3.advisor.SafetyAdvisor;
import com.skala.lab0.myapp.lab3.advisor.TokenMeterAdvisor;
import com.skala.lab0.myapp.lab3.tools.OrderTools;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class Lab3AiConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient assistantChatClient(
            ChatClient.Builder builder,
            VectorStore vs,
            ChatMemory memory,
            MeterRegistry meterRegistry,
            OrderTools tools
    ) {
        var clientBuilder = builder
                .defaultSystem("""
                        당신은 쇼핑몰 상담 에이전트입니다.
                        주문 상태 조회와 환불 접수는 추측하지 말고 반드시 등록된 도구를 사용하세요.
                        현재 요청에서 생략된 주문번호나 환불 사유가 같은 대화의 이전 발화에 있으면 그 값을 사용하세요.
                        "그거"처럼 이전 대상을 가리키는 후속 질문에는 해석한 주문번호나 상품명을 답변에 밝혀 주세요.
                        앞에서 단순 변심 반품 가능 여부를 물은 뒤 같은 주문의 환불 접수를 요청하면 환불 사유는 "단순 변심"으로 사용하세요.
                        대화에서도 필요한 값을 확인할 수 없을 때만 사용자에게 되물으세요.
                        """)
                .defaultAdvisors(
                        new AuditAdvisor(meterRegistry),                    // order 0 가장 바깥
                        new SafetyAdvisor(),                                // order 100 차단
                        MessageChatMemoryAdvisor.builder(memory).build(),   // order 200 기억
                        new RagAdvisor(vs),                                 // order 300 근거 검색
                        new TokenMeterAdvisor(meterRegistry)                // order 900 계측
                );

        clientBuilder.defaultTools(tools);

        return clientBuilder.build();
    }
}
