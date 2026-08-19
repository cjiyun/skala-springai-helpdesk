package com.skala.lab0.myapp.rag;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@Tag(name="Lab 2 - 문서 Q&A")

public class Lab2AskController {

    private final AnswerService answerService;

    // AnswerService를 주입받습니다. 
    public Lab2AskController(AnswerService answerService){
        this.answerService = answerService;
    }

    @PostMapping("/lab2/ask")
    @Operation(summary = "step 3 - 근거로 답하기", description = "근거 기반의 답변(AnswerDto)을 반환합니다.")
    public AnswerDto ask(@RequestParam String q){
        return answerService.ask(q);
    }
}