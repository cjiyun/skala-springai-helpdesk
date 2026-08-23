package com.skala.lab0.myapp.lab3.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;

import com.skala.lab0.myapp.lab3.advisor.AuditAdvisor;
import com.skala.lab0.myapp.lab3.advisor.RagAdvisor;
import com.skala.lab0.myapp.lab3.advisor.SafetyAdvisor;
import com.skala.lab0.myapp.lab3.advisor.TokenMeterAdvisor;
import com.skala.lab0.myapp.lab3.tools.OrderTools;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class Lab3AiConfig {

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository repository,
            @Value("${helpdesk.memory.max:20}") int maxMessages) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(maxMessages)
                .build();
    }

    @Bean
    public ChatClient assistantChatClient(
            ChatClient.Builder builder,
            VectorStore vs,
            ChatMemory memory,
            MeterRegistry meterRegistry,
            OrderTools tools,
            @Value("${helpdesk.rag.top-k:2}") int topK,
            @Value("${helpdesk.rag.threshold:0.29}") double threshold,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String primaryModel,
            @Value("${helpdesk.model.fallback:gpt-4.1-mini}") String fallbackModel,
            @Value("${helpdesk.cost.primary.input-per-million-usd:0.15}") double primaryInputRate,
            @Value("${helpdesk.cost.primary.output-per-million-usd:0.60}") double primaryOutputRate,
            @Value("${helpdesk.cost.fallback.input-per-million-usd:0.40}") double fallbackInputRate,
            @Value("${helpdesk.cost.fallback.output-per-million-usd:1.60}") double fallbackOutputRate
    ) {
        var clientBuilder = builder
                .defaultSystem(new ClassPathResource("prompts/helpdesk-system.txt"))
                .defaultAdvisors(
                        new SafetyAdvisor(),                                // order 0 차단
                        new AuditAdvisor(meterRegistry),                    // order 100 감사
                        MessageChatMemoryAdvisor.builder(memory).order(200).build(),
                        new RagAdvisor(vs, topK, threshold),                // order 300 근거 검색
                        new TokenMeterAdvisor(meterRegistry, primaryModel, fallbackModel,
                                primaryInputRate, primaryOutputRate, fallbackInputRate, fallbackOutputRate)
                );

        clientBuilder.defaultTools(tools);

        return clientBuilder.build();
    }
}
