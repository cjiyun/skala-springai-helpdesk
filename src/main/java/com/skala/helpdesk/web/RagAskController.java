package com.skala.helpdesk.web;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.skala.helpdesk.rag.AnswerService;
import com.skala.helpdesk.rag.dto.RagAnswerDto;

@RestController
@Tag(name="Lab 2 - 문서 Q&A")
public class RagAskController {

    private final AnswerService answerService;

    public RagAskController(AnswerService answerService){
        this.answerService = answerService;
    }

    @PostMapping("/lab2/ask")
    @Operation(summary = "step 3 - 근거로 답하기", description = "근거 기반의 답변(RagAnswerDto)을 반환합니다.")
    public RagAnswerDto ask(@RequestParam String q){
        return answerService.ask(q);
    }
}