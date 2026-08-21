package com.skala.lab0.myapp.lab3.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;

import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Flux;
import java.util.concurrent.atomic.AtomicLong;

public class TokenMeterAdvisor implements CallAdvisor, StreamAdvisor {

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
                long total = 0;
                //프롬프트 토큰 계측
                if (usage.getPromptTokens() != null){
                    meterRegistry.counter("ai.tokens", "type", "prompt", "feature", "chat")
                        .increment(usage.getPromptTokens());
                    total += usage.getPromptTokens();
                }
                // 생성(응답) 토큰 계측
                if (usage.getCompletionTokens() != null) {
                    meterRegistry.counter("ai.tokens", "type", "generation", "feature", "chat")
                        .increment(usage.getCompletionTokens());
                    total += usage.getCompletionTokens();
                }
                meterRegistry.summary("ai.tokens.per.request", "feature", "chat").record(total);
            }
        }

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        AtomicLong prompt = new AtomicLong();
        AtomicLong completion = new AtomicLong();
        return chain.nextStream(request)
                .doOnNext(response -> {
                    if (response.chatResponse() == null || response.chatResponse().getMetadata() == null) return;
                    Usage usage = response.chatResponse().getMetadata().getUsage();
                    if (usage != null) {
                        if (usage.getPromptTokens() != null) prompt.set(usage.getPromptTokens());
                        if (usage.getCompletionTokens() != null) completion.set(usage.getCompletionTokens());
                    }
                })
                .doOnComplete(() -> {
                    meterRegistry.counter("ai.tokens", "type", "prompt", "feature", "chat-stream").increment(prompt.get());
                    meterRegistry.counter("ai.tokens", "type", "generation", "feature", "chat-stream").increment(completion.get());
                    meterRegistry.summary("ai.tokens.per.request", "feature", "chat-stream")
                            .record(prompt.get() + completion.get());
                });
    }
}
