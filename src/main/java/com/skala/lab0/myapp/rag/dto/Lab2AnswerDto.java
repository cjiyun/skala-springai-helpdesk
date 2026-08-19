package com.skala.lab0.myapp.rag.dto;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.skala.lab0.myapp.rag.service.Lab2SearchService;

@Service
public class Lab2AnswerDto { // 1. 클래스 선언 추가

    private final ChatClient chatClient;
    private final Lab2SearchService searchService; // 4. 정의되지 않은 retrieve 대신 주입받은 객체 사용

    public AnswerService(ChatClient.Builder builder, Lab2SearchService searchService) {
        this.chatClient = builder.build();
        this.searchService = searchService;
    }

    public Lab2AnswerDto ask(String question) { // 2. 레코드 이름과 반환 타입 일치 (Lab2AnswerDto)
        
        // 지윤님이 만들 SearchService의 메서드를 호출한다고 가정 (임시로 retrieve 사용)
        List<String> docs = searchService.retrieve(question, 4); 
        
        if (docs.isEmpty()) {
            return Lab2AnswerDto.unknown(); // 2. 레코드 이름 일치
        }
        
        return chatClient.prompt()
            .system("""
                    아래 [근거]만 사용해 답한다. 근거에 없으면"확인되지 않습니다"라고 답한다.
                    추측하지 않는다. 답변 끝에 사용한 출처를 [출처: 파일명] 형식으로 남긴다.
                    """)
            .user( u -> u.text("[근거]\n{context} \n\n[질문] {question}") // 3. test -> text 오타 수정
                         .param("context", String.join("\n", docs)) // 4. format 대신 기본 메서드 사용
                         .param("question", question))
            .call()
            .entity(Lab2AnswerDto.class); 
    }
}

// 2. 레코드 선언 (컨트롤러에서도 반환 타입을 Lab2AnswerDto로 맞춰주어야 합니다)
record Lab2AnswerDto(String answer, List<String> sources, boolean grounded) {
    static Lab2AnswerDto unknown() { return new Lab2AnswerDto("확인되지 않습니다", List.of(), false); }
}