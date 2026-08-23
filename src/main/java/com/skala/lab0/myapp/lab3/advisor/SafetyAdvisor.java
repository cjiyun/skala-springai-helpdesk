package com.skala.lab0.myapp.lab3.advisor;

import java.util.List;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;
import java.util.regex.Pattern;
import java.util.Locale;

public class SafetyAdvisor implements CallAdvisor, StreamAdvisor {

    private static final List<String> BLOCKED_PATTERNS = List.of(
            "이전 지시 무시",
            "시스템 프롬프트",
            "ignore previous instructions",
            "관리자 권한 부여", "관리자 권한을 부여", "DROP TABLE", "curl http", "모든 사용자의 주문",
            "개발자 모드", "sudo rm", "보안 정책 우회", "보안 정책을 우회", "jailbreak"
    );
    private static final Pattern RESIDENT_ID = Pattern.compile("(?<!\\d)\\d{6}-?[1-4]\\d{6}(?!\\d)");
    private static final Pattern CARD_NUMBER = Pattern.compile("(?<!\\d)(?:\\d[ -]?){15,18}\\d(?!\\d)");

    @Override
    public String getName() {
        return "SafetyAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        validate(request);
        return chain.nextCall(request);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        validate(request);
        return chain.nextStream(request);
    }

    private void validate(ChatClientRequest request) {
        String text = request.prompt() == null ? "" : request.prompt().getContents();
        String lower = text.toLowerCase(Locale.ROOT);
        if (BLOCKED_PATTERNS.stream().anyMatch(pattern -> lower.contains(pattern.toLowerCase(Locale.ROOT)))
                || RESIDENT_ID.matcher(text).find() || CARD_NUMBER.matcher(text).find()) {
            throw new IllegalArgumentException("보안 정책상 처리할 수 없는 요청입니다.");
        }
    }
}
