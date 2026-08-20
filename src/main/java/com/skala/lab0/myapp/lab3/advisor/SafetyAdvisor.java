package com.skala.lab0.myapp.lab3.advisor;

import java.util.List;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

public class SafetyAdvisor implements CallAdvisor {

    private static final List<String> BLOCKED_PATTERNS = List.of(
            "이전 지시 무시",
            "시스템 프롬프트",
            "ignore previous instructions",
            "관리자 권한 부여"
    );

    @Override
    public String getName() {
        return "SafetyAdvisor";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String userText = request.prompt() != null ? request.prompt().getContents() : null;

        if (userText != null) {
            for (String pattern : BLOCKED_PATTERNS) {
                if (userText.contains(pattern)) {
                    throw new IllegalArgumentException("보안 정책상 처리할 수 없는 요청입니다: " + pattern);
                }
            }
        }

        return chain.nextCall(request);
    }
}