package com.skala.lab0.myapp.rag.dto;

import java.util.List;

public record Lab2AnswerDto(String answer, List<String> sources, boolean grounded) {
    public static Lab2AnswerDto unknown() {
        return new Lab2AnswerDto("확인되지 않습니다", List.of(), false);
    }
} 
