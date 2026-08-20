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
            MeterRegistry meterRegistry
            // TODO: 지윤님 OrderTools 완성 후 주석 해제
            // , com.skala.lab0.myapp.lab3.tools.OrderTools tools
    ) {
        var clientBuilder = builder
                .defaultAdvisors(
                        new AuditAdvisor(meterRegistry),                    // order 0 가장 바깥
                        new SafetyAdvisor(),                                // order 100 차단
                        MessageChatMemoryAdvisor.builder(memory).build(),   // order 200 기억
                        new RagAdvisor(vs),                                 // order 300 근거 검색
                        new TokenMeterAdvisor(meterRegistry)                // order 900 계측
                );

        // TODO: 지윤님 OrderTools 완성 후 주석 해제
        // clientBuilder.defaultTools(tools);

        return clientBuilder.build();
    }
}