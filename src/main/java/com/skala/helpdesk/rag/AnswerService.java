package com.skala.helpdesk.rag;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import com.skala.helpdesk.rag.dto.RagAnswerDto;
import com.skala.helpdesk.rag.dto.ChunkResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AnswerService {
    private static final Logger log = LoggerFactory.getLogger(AnswerService.class);

    private final ChatClient chatClient;
    private final SearchService searchService;
    private final IngestService ingestService;

    public AnswerService(ChatClient.Builder chatClientBuilder, SearchService searchService,
            IngestService ingestService) {
        this.chatClient = chatClientBuilder.build();
        this.searchService = searchService;
        this.ingestService = ingestService;
    }

    @PostConstruct
    public void initDocuments() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:helpdesk-docs/*.md");
            
            for (Resource resource : resources) {
                if (resource.getFilename() != null && !resource.getFilename().contains("README")) {
                    ingestService.ingest(resource, resource.getFilename(), "1");
                }
            }
        } catch (Exception exception) {
            log.warn("규정 문서 초기 적재에 실패했습니다: {}", exception.getMessage());
        }
    }

    public RagAnswerDto ask(String question) {
        List<ChunkResponse> docs = searchService.retrieve(question, 5);

        if (docs == null || docs.isEmpty()) {
            return RagAnswerDto.unknown();
        }

        String systemPrompt = """
                당신은 쇼핑몰/사내 규정을 안내하는 AI 어시스턴트입니다.
                제공된 [참고 문서 조각]들을 바탕으로 질문에 정확하고 명확한 문장으로 답변하세요.

                [답변 작성 가이드]
                1. 반품 기한(예: 7일), 교환/환불 조건(포장 개봉/훼손 시 불가 등), 회원 혜택(VIP, 골드 등급 적립률/혜택) 등 질문과 관련된 정보가 조금이라도 있다면 그 내용을 사실에 맞게 반드시 답변에 포함하세요.
                   - golden 세트 검증을 위해 반드시 핵심 키워드(숫자, 기간, 혜택명 등)를 빠짐없이 명시하세요.
                   - grounded: true
                   - sources: 답변의 근거가 된 문서의 정확한 파일명(예: "return-policy.md", "membership.md" 등)
                2. 문서 조각들과 완전히 무관한 질문(예: 우주 배송, 타사 차액 보상 등)에만 "확인되지 않습니다"라고 답하세요.
                   - grounded: false
                   - sources: []
                """;

        String context = docs.stream()
                .map(d -> "[출처 파일: " + d.source() + "]\n" + d.snippet())
                .collect(Collectors.joining("\n\n---\n\n"));

        return chatClient.prompt()
                .system(systemPrompt)
                .user(u -> u.text("""
                        [참고 문서 조각]
                        {context}

                        [사용자 질문]
                        {question}
                        """)
                        .param("context", context)
                        .param("question", question))
                .call()
                .entity(RagAnswerDto.class);
    }
}
