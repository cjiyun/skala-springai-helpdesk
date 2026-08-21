package com.skala.lab0.myapp.lab3.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;

import io.micrometer.core.instrument.MeterRegistry;

public class TokenMeterAdvisor implements CallAdvisor {

    private final MeterRegistry meterRegistry;

    public TokenMeterAdvisor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String getName() {
        return "TokenMeterAdvisor";
    }

    @Override
    public int getOrder() {
        return 900;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);

        if (response.chatResponse() != null && response.chatResponse().getMetadata()!=null){
            Usage usage = response.chatResponse().getMetadata().getUsage();
            if (usage != null){
                //프롬프트 토큰 계측
                if (usage.getPromptTokens() != null){
                    meterRegistry.counter("ai.tokens", "type", "prompt", "feature", "chat")
                        .increment(usage.getPromptTokens());
                }
                // 생성(응답) 토큰 계측
                if (usage.getCompletionTokens() != null) {
                    meterRegistry.counter("ai.tokens", "type", "generation", "feature", "chat")
                        .increment(usage.getCompletionTokens());
                }
            }
        }

        return response;
    }
}
