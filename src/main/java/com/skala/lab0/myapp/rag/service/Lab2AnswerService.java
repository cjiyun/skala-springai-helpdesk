package com.skala.lab0.myapp.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AnswerService {

    private final ChatClient chatClient;
    private final SearchService searchService;

    public AnswerService(ChatClient.Builder chatClientBuilder, SearchService searchService) {
        this.chatClient = chatClientBuilder.build();
        this.searchService = searchService;
    }

    public AnswerDto ask(String question) {
        // 1. 검색 서비스 호출
        String context = searchService.search(question);


        // 2. 시스템 프롬프트 구성 (환각 통제 및 DTO 매핑 지시)
        String systemPrompt="""
                당신은 사내 규정을 안내하는 AI 어시스턴트입니다.
                반드시 아래 제공된 [검색된 문서]를 바탕으로 사용자의 질문에 답변하십시오.
                
                [지시사항]
                1. 검색된 문서에 질문과 관련된 내용이 없다면, 절대 지어내지 말고 "확인되지 않습니다" 또는 "모릅니다"라고 답변하십시오.
                2. 문서의 내용을 바탕으로 답변을 작성했다면 usedEvidence를 true로, 그렇지 않다면 false로 설정하십시오.
                3. 참고한 문서의 출처(파일명 등)를 sources 배열에 포함하십시오.
                
                [검색된 문서]
                {context}
                """;
    
        // 3. llm 호출 및 DTO 구조화 반환
        return chatClient.prompt()
                .system(s -> s.text(systemPrompt)
                                .param("context", context))
                .user(question)
                .call()
                .entity(AnswerDto.class); // Spring AI가 JSON 결과를 AnswerDto 객체로 파싱
    }
}