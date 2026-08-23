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
import java.util.concurrent.atomic.AtomicReference;

public class TokenMeterAdvisor implements CallAdvisor, StreamAdvisor {

    private final MeterRegistry meterRegistry;
    private final String primaryModel;
    private final String fallbackModel;
    private final double primaryInputRate;
    private final double primaryOutputRate;
    private final double fallbackInputRate;
    private final double fallbackOutputRate;

    public TokenMeterAdvisor(MeterRegistry meterRegistry, String primaryModel, String fallbackModel,
            double primaryInputRate, double primaryOutputRate,
            double fallbackInputRate, double fallbackOutputRate) {
        this.meterRegistry = meterRegistry;
        this.primaryModel = primaryModel;
        this.fallbackModel = fallbackModel;
        this.primaryInputRate = primaryInputRate;
        this.primaryOutputRate = primaryOutputRate;
        this.fallbackInputRate = fallbackInputRate;
        this.fallbackOutputRate = fallbackOutputRate;
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
                record("chat", response.chatResponse().getMetadata().getModel(),
                        value(usage.getPromptTokens()), value(usage.getCompletionTokens()));
            }
        }

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        AtomicLong prompt = new AtomicLong();
        AtomicLong completion = new AtomicLong();
        AtomicReference<String> model = new AtomicReference<>(primaryModel);
        return chain.nextStream(request)
                .doOnNext(response -> {
                    if (response.chatResponse() == null || response.chatResponse().getMetadata() == null) return;
                    Usage usage = response.chatResponse().getMetadata().getUsage();
                    if (response.chatResponse().getMetadata().getModel() != null) {
                        model.set(response.chatResponse().getMetadata().getModel());
                    }
                    if (usage != null) {
                        if (usage.getPromptTokens() != null) prompt.set(usage.getPromptTokens());
                        if (usage.getCompletionTokens() != null) completion.set(usage.getCompletionTokens());
                    }
                })
                .doOnComplete(() -> record("chat-stream", model.get(), prompt.get(), completion.get()));
    }

    private void record(String feature, String responseModel, long input, long output) {
        String model = responseModel == null || responseModel.isBlank() ? primaryModel : responseModel;
        long total = input + output;
        meterRegistry.counter("ai.tokens", "type", "input", "feature", feature, "model", model).increment(input);
        meterRegistry.counter("ai.tokens", "type", "output", "feature", feature, "model", model).increment(output);
        meterRegistry.counter("ai.tokens", "type", "total", "feature", feature, "model", model).increment(total);
        meterRegistry.summary("ai.tokens.per.request", "feature", feature, "model", model).record(total);

        boolean fallback = model.startsWith(fallbackModel);
        double inputRate = fallback ? fallbackInputRate : primaryInputRate;
        double outputRate = fallback ? fallbackOutputRate : primaryOutputRate;
        double estimatedUsd = (input * inputRate + output * outputRate) / 1_000_000d;
        meterRegistry.counter("ai.cost.estimated.usd", "feature", feature, "model", model).increment(estimatedUsd);
    }

    private long value(Integer tokens) {
        return tokens == null ? 0 : tokens;
    }
}
