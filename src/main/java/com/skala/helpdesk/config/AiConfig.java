package com.skala.helpdesk.config;

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
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.skala.helpdesk.advisor.AuditAdvisor;
import com.skala.helpdesk.advisor.RagAdvisor;
import com.skala.helpdesk.advisor.SafetyAdvisor;
import com.skala.helpdesk.advisor.TokenMeterAdvisor;
import com.skala.helpdesk.tools.OrderTools;
import com.skala.helpdesk.tools.TicketTools;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
@EnableConfigurationProperties(HelpDeskProperties.class)
public class AiConfig {

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository repository, HelpDeskProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(properties.memory().max())
                .build();
    }

    @Bean
    public ChatClient assistantChatClient(
            ChatClient.Builder builder,
            VectorStore vs,
            ChatMemory memory,
            MeterRegistry meterRegistry,
            OrderTools orderTools,
            TicketTools ticketTools,
            HelpDeskProperties properties,
            @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String primaryModel
    ) {
        String fallbackModel = properties.model().fallback();
        var primaryRates = properties.cost().primary();
        var fallbackRates = properties.cost().fallback();
        var clientBuilder = builder
                .defaultSystem(new ClassPathResource("prompts/helpdesk-system.txt"))
                .defaultAdvisors(
                        new SafetyAdvisor(),                                // order 0 차단
                        new AuditAdvisor(meterRegistry),                    // order 100 감사
                        MessageChatMemoryAdvisor.builder(memory).order(200).build(),
                        new RagAdvisor(vs, properties.rag().topK(), properties.rag().threshold()),
                        new TokenMeterAdvisor(meterRegistry, primaryModel, fallbackModel,
                                primaryRates.inputPerMillionUsd(), primaryRates.outputPerMillionUsd(),
                                fallbackRates.inputPerMillionUsd(), fallbackRates.outputPerMillionUsd())
                );

        clientBuilder.defaultTools(orderTools, ticketTools);

        return clientBuilder.build();
    }
}
