package com.skala.lab0.myapp.lab3.advisor;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public class AuditAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AuditAdvisor.class);
    private static final String TRACE_ID_KEY = "traceId";

    private final MeterRegistry meterRegistry;

    public AuditAdvisor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public String getName() {
        return "AuditAdvisor";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 1. Trace ID 생성 및 MDC 등록
        String traceId = MDC.get(TRACE_ID_KEY);
        boolean ownsTraceId = traceId == null;
        if (ownsTraceId) {
            traceId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put(TRACE_ID_KEY, traceId);
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        long startTime = System.currentTimeMillis();
        String userText = request.prompt() != null ? request.prompt().getContents() : "";

        log.info("[AUDIT-REQ] User Prompt: {}", userText);

        try {
            // 2. 체인 실행
            ChatClientResponse response = chain.nextCall(request);
            long elapsed = System.currentTimeMillis() - startTime;

            // 3. 지연 메트릭 기록 (성공)
            sample.stop(Timer.builder("ai.latency")
                    .tag("phase", "model")
                    .tag("status", "success")
                    .tag("feature", "chat")
                    .register(meterRegistry));
            
            String output = "";
            if (response.chatResponse() != null && response.chatResponse().getResult() != null) {
                output = response.chatResponse().getResult().getOutput().getText();
            }
            
            log.info("[AUDIT-RES] [traceId={}] Latency: {}ms | Response: {}", traceId, elapsed, output);
            return response;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;

            //3. 지연 메트릭 기록 (실패)
            sample.stop(Timer.builder("ai.latency")
                    .tag("phase", "model")
                    .tag("status", "error")
                    .tag("feature", "chat")
                    .register(meterRegistry));

            log.warn("[AUDIT-FAIL] [traceId={}] Latency: {}ms | Error: {}", traceId, elapsed, e.getMessage());
            throw e;
        } finally {
            // 4. 컨텍스트 정리
            if (ownsTraceId) {
                MDC.remove(TRACE_ID_KEY);
            }
        }
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        Timer.Sample sample = Timer.start(meterRegistry);
        Timer success = Timer.builder("ai.latency").tag("phase", "model").tag("status", "success")
                .tag("feature", "chat-stream").register(meterRegistry);
        Timer error = Timer.builder("ai.latency").tag("phase", "model").tag("status", "error")
                .tag("feature", "chat-stream").register(meterRegistry);
        return chain.nextStream(request)
                .doOnComplete(() -> sample.stop(success))
                .doOnError(exception -> sample.stop(error));
    }
}
