package com.skala.lab0.myapp.lab3.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;

import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIException;
import com.openai.errors.BadRequestException;
import com.openai.errors.UnauthorizedException;
import com.skala.lab0.myapp.lab3.tools.ToolUsage;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class Lab3ChatServiceFallbackTest {
  private ChatClient chatClient;
  private ChatClient.ChatClientRequestSpec primary;
  private ChatClient.ChatClientRequestSpec fallback;
  private ChatClient.CallResponseSpec primaryCall;
  private ChatClient.CallResponseSpec fallbackCall;
  private ChatClient.StreamResponseSpec primaryStream;
  private ChatClient.StreamResponseSpec fallbackStream;
  private SimpleMeterRegistry meters;

  @BeforeEach
  void setUp() {
    chatClient = mock(ChatClient.class);
    primary = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
    fallback = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
    primaryCall = mock(ChatClient.CallResponseSpec.class);
    fallbackCall = mock(ChatClient.CallResponseSpec.class);
    primaryStream = mock(ChatClient.StreamResponseSpec.class);
    fallbackStream = mock(ChatClient.StreamResponseSpec.class);
    meters = new SimpleMeterRegistry();
    when(chatClient.prompt()).thenReturn(primary, fallback);
    when(primary.call()).thenReturn(primaryCall);
    when(fallback.call()).thenReturn(fallbackCall);
    when(primary.stream()).thenReturn(primaryStream);
    when(fallback.stream()).thenReturn(fallbackStream);
  }

  @Test
  void 주_모델만_실패하면_폴백_응답과_메트릭을_반환한다() {
    when(primaryCall.chatClientResponse()).thenThrow(new OpenAIIoException("primary failed"));
    ChatClientResponse fallbackResponse = response("fallback answer");
    when(fallbackCall.chatClientResponse()).thenReturn(fallbackResponse);
    Lab3ChatService service = service();

    AnswerDto answer = service.chat("user1", "s1", "hello");

    assertThat(answer.answer()).isEqualTo("fallback answer");
    verify(fallback).options(any(OpenAiChatOptions.Builder.class));
    assertThat(meters.get("ai.fallback.calls").tag("feature", "chat").counter().count()).isEqualTo(1);
    assertThat(meters.get("ai.fallback.outcomes").tags("feature", "chat", "result", "success")
        .counter().count()).isEqualTo(1);
  }

  @Test
  void 폴백_모델도_실패하면_실패_메트릭을_기록한다() {
    when(primaryCall.chatClientResponse()).thenThrow(new OpenAIIoException("primary failed"));
    when(fallbackCall.chatClientResponse()).thenThrow(new OpenAIIoException("fallback failed"));

    assertThatThrownBy(() -> service().chat("user1", "s1", "hello"))
        .isInstanceOf(OpenAIIoException.class).hasMessageContaining("fallback failed");

    assertThat(meters.get("ai.fallback.calls").tag("feature", "chat").counter().count()).isEqualTo(1);
    assertThat(meters.get("ai.fallback.outcomes").tags("feature", "chat", "result", "error")
        .counter().count()).isEqualTo(1);
  }

  @Test
  void 스트림이_시작되기_전_일시_오류는_폴백한다() {
    ChatClientResponse fallbackResponse = response("fallback token");
    when(primaryStream.chatClientResponse()).thenReturn(reactor.core.publisher.Flux.error(
        new OpenAIIoException("primary failed")));
    when(fallbackStream.chatClientResponse()).thenReturn(reactor.core.publisher.Flux.just(fallbackResponse));

    var responses = service().stream("user1", "s1", "hello", new ToolUsage()).collectList().block();

    assertThat(responses).containsExactly(fallbackResponse);
    verify(fallback).options(any(OpenAiChatOptions.Builder.class));
    assertThat(meters.get("ai.fallback.calls").tag("feature", "chat-stream").counter().count()).isEqualTo(1);
    assertThat(meters.get("ai.fallback.outcomes").tags("feature", "chat-stream", "result", "success")
        .counter().count()).isEqualTo(1);
  }

  @Test
  void 첫_token_후_오류는_폴백_응답과_섞지_않는다() {
    ChatClientResponse partial = response("partial");
    when(primaryStream.chatClientResponse()).thenReturn(reactor.core.publisher.Flux.concat(
        reactor.core.publisher.Flux.just(partial),
        reactor.core.publisher.Flux.error(new OpenAIIoException("failed after token"))));
    var responses = new ArrayList<ChatClientResponse>();
    var failure = new AtomicReference<Throwable>();

    service().stream("user1", "s1", "hello", new ToolUsage()).subscribe(responses::add, failure::set);

    assertThat(responses).containsExactly(partial);
    assertThat(failure.get()).isInstanceOf(OpenAIIoException.class);
    verify(fallback, never()).options(any(OpenAiChatOptions.Builder.class));
    assertThat(meters.find("ai.fallback.calls").counter()).isNull();
  }

  @Test
  void 쓰기_Tool_실행_후_실패하면_폴백하지_않아_중복_생성을_막는다() {
    when(primary.toolContext(any())).thenAnswer(invocation -> {
      Map<?, ?> context = invocation.getArgument(0);
      ((ToolUsage) context.get(ToolUsage.CONTEXT_KEY)).markWriteUsed();
      return primary;
    });
    when(primaryCall.chatClientResponse()).thenThrow(new OpenAIIoException("failed after write"));

    assertThatThrownBy(() -> service().chat("user1", "s1", "교환 접수"))
        .isInstanceOf(OpenAIException.class);

    verify(fallback, never()).call();
    assertThat(meters.find("ai.fallback.calls").counter()).isNull();
  }

  @Test
  void 인증_오류는_폴백으로_재시도하지_않는다() {
    when(primaryCall.chatClientResponse()).thenThrow(mock(UnauthorizedException.class));

    assertThatThrownBy(() -> service().chat("user1", "s1", "hello"))
        .isInstanceOf(UnauthorizedException.class);

    verify(fallback, never()).call();
    assertThat(meters.find("ai.fallback.calls").counter()).isNull();
  }

  @Test
  void 입력_오류는_폴백으로_재시도하지_않는다() {
    when(primaryCall.chatClientResponse()).thenThrow(mock(BadRequestException.class));

    assertThatThrownBy(() -> service().chat("user1", "s1", "invalid"))
        .isInstanceOf(BadRequestException.class);

    verify(fallback, never()).call();
    assertThat(meters.find("ai.fallback.calls").counter()).isNull();
  }

  @Test
  void 스트림도_쓰기_Tool_실행_후에는_폴백하지_않는다() {
    when(primaryStream.chatClientResponse()).thenReturn(reactor.core.publisher.Flux.error(
        new OpenAIIoException("failed after write")));
    ToolUsage usage = new ToolUsage();
    usage.markWriteUsed();
    var failure = new AtomicReference<Throwable>();

    service().stream("user1", "s1", "교환 접수", usage).subscribe(ignored -> {}, failure::set);

    assertThat(failure.get()).isInstanceOf(OpenAIIoException.class);
    verify(fallback, never()).stream();
    assertThat(meters.find("ai.fallback.calls").counter()).isNull();
  }

  private Lab3ChatService service() {
    return new Lab3ChatService(chatClient, mock(ChatMemory.class), "fallback-model", "skala", meters);
  }

  private ChatClientResponse response(String text) {
    ChatClientResponse response = mock(ChatClientResponse.class, RETURNS_DEEP_STUBS);
    when(response.chatResponse().getResult().getOutput().getText()).thenReturn(text);
    when(response.context()).thenReturn(Map.of());
    return response;
  }
}
