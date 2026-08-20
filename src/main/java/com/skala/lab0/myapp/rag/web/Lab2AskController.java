package com.skala.lab0.myapp.rag.web;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.skala.lab0.myapp.rag.service.Lab2AnswerService;
import com.skala.lab0.myapp.rag.dto.Lab2AnswerDto;

@RestController
@Tag(name="Lab 2 - 문서 Q&A")
public class Lab2AskController {

    private final Lab2AnswerService answerService;

    public Lab2AskController(Lab2AnswerService answerService){
        this.answerService = answerService;
    }

    @PostMapping("/lab2/ask")
    @Operation(summary = "step 3 - 근거로 답하기", description = "근거 기반의 답변(Lab2AnswerDto)을 반환합니다.")
    public Lab2AnswerDto ask(@RequestParam String q){
        return answerService.ask(q);
    }
}