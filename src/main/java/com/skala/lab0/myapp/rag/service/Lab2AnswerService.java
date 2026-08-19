package com.skala.lab0.myapp.rag.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.skala.lab0.myapp.rag.dto.Lab2AnswerDto;
import com.skala.lab0.myapp.rag.dto.Lab2ChunkResponse;

@Service
public class Lab2AnswerService {

    private final ChatClient chatClient;
    private final Lab2SearchService searchService; 

    public Lab2AnswerService(ChatClient.Builder chatClientBuilder, Lab2SearchService searchService) {
        this.chatClient = chatClientBuilder.build();
        this.searchService = searchService;
    }

    public Lab2AnswerDto ask(String question) {
        // 팀원이 작성한 Lab2SearchService의 retrieve 메서드를 호출한다고 가정합니다.
        List<Lab2ChunkResponse> docs = searchService.retrieve(question, 4); 

        if (docs == null || docs.isEmpty()) {
            return Lab2AnswerDto.unknown();
        }

        String systemPrompt="""
                당신은 사내 규정을 안내하는 AI 어시스턴트입니다.
                반드시 아래 제공된 [검색된 문서]를 바탕으로 사용자의 질문에 답변하십시오.
                
                [지시사항]
                1. 검색된 문서에 질문과 관련된 내용이 없다면, 절대 지어내지 말고 "확인되지 않습니다" 또는 "모릅니다"라고 답변하십시오.
                2. 문서의 내용을 바탕으로 답변을 작성했다면 grounded를 true로, 그렇지 않다면 false로 설정하십시오.
                3. 참고한 문서의 출처(파일명 등)를 sources 배열에 포함하십시오.
                """;
    
        return chatClient.prompt()
                .system(s -> s.text(systemPrompt))
                .user(u -> u.text("[근거]\n{context} \n\n[질문] {question}")
                             .param("context", docs.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining("\n")))
                             .param("question", question))
                .call()
                .entity(Lab2AnswerDto.class); 
    }
}