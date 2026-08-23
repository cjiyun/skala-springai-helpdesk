package com.skala.lab0.myapp.lab3.advisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Flux;

class TokenMeterAdvisorTest {
  private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
  private final TokenMeterAdvisor advisor = new TokenMeterAdvisor(
      meters, "gpt-4o-mini", "gpt-4.1-mini", 0.15, 0.60, 0.40, 1.60);

  @Test
  void 동기_응답의_모델별_입출력_전체_토큰과_예상_비용을_기록한다() {
    ChatClientRequest request = mock(ChatClientRequest.class);
    ChatClientResponse response = response("gpt-4o-mini-2024-07-18", 1_000, 500);
    CallAdvisorChain chain = mock(CallAdvisorChain.class);
    when(chain.nextCall(request)).thenReturn(response);

    assertThat(advisor.adviseCall(request, chain)).isSameAs(response);

    assertThat(tokens("input", "chat", "gpt-4o-mini-2024-07-18")).isEqualTo(1_000);
    assertThat(tokens("output", "chat", "gpt-4o-mini-2024-07-18")).isEqualTo(500);
    assertThat(tokens("total", "chat", "gpt-4o-mini-2024-07-18")).isEqualTo(1_500);
    assertThat(cost("chat", "gpt-4o-mini-2024-07-18")).isEqualTo(0.00045);
  }

  @Test
  void 스트림은_마지막_usage로_폴백_모델_비용을_한_번만_기록한다() {
    ChatClientRequest request = mock(ChatClientRequest.class);
    StreamAdvisorChain chain = mock(StreamAdvisorChain.class);
    ChatClientResponse first = response("gpt-4.1-mini-2025-04-14", 100, 0);
    ChatClientResponse last = response("gpt-4.1-mini-2025-04-14", 100, 20);
    when(chain.nextStream(request)).thenReturn(Flux.just(first, last));

    advisor.adviseStream(request, chain).blockLast();

    assertThat(tokens("total", "chat-stream", "gpt-4.1-mini-2025-04-14")).isEqualTo(120);
    assertThat(cost("chat-stream", "gpt-4.1-mini-2025-04-14")).isEqualTo(0.000072);
  }

  private ChatClientResponse response(String model, Integer input, Integer output) {
    ChatClientResponse response = mock(ChatClientResponse.class);
    ChatResponse chatResponse = mock(ChatResponse.class);
    ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
    Usage usage = mock(Usage.class);
    when(response.chatResponse()).thenReturn(chatResponse);
    when(chatResponse.getMetadata()).thenReturn(metadata);
    when(metadata.getModel()).thenReturn(model);
    when(metadata.getUsage()).thenReturn(usage);
    when(usage.getPromptTokens()).thenReturn(input);
    when(usage.getCompletionTokens()).thenReturn(output);
    return response;
  }

  private double tokens(String type, String feature, String model) {
    return meters.get("ai.tokens").tags("type", type, "feature", feature, "model", model).counter().count();
  }

  private double cost(String feature, String model) {
    return meters.get("ai.cost.estimated.usd").tags("feature", feature, "model", model).counter().count();
  }
}
