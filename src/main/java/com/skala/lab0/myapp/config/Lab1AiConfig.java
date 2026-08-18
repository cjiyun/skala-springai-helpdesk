package com.skala.lab0.myapp.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Lab1AiConfig {
  @Bean
  ChatClient summaryChatClient(ChatClient.Builder builder) {
    return builder
      .defaultSystem("""
        너는 이커머스 주문 상담 도우미다.
        주어진 주문 정보만 사용해 한국어 한 문장으로 요약한다.
        추측하지 않는다. 정보가 부족하면 "정보가 부족합니다"라고 답한다.
      """)
      .defaultOptions(ChatOptions.builder()
        .temperature(0.0)
        .maxTokens(120)
      )
      .build();
  }
}
