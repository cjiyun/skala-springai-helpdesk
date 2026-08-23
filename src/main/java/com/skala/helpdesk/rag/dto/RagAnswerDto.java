package com.skala.helpdesk.rag.dto;

import java.util.List;

public record RagAnswerDto(String answer, List<String> sources, boolean grounded) {
    public static RagAnswerDto unknown() { 
        return new RagAnswerDto("확인되지 않습니다", List.of(), false); 
    }
}