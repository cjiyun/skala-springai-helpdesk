package com.skala.lab0.myapp.lab3.chat;

import java.util.List;
import com.skala.lab0.myapp.rag.dto.Lab2AnswerDto;

public record Lab3ChatResponse (
    String answer, //최종 답변 내용
    List<String> sources, // RAG 출처 - Day2 연결
    boolean grounded, // 문서 기반 답변 여부
    String ticketNo, // 환불 접수 시 생성된 티켓 번호
    List<String> history
) {
    // 기본 생성자 편의 오버로딩(기존 4개 필드 호환)
    public Lab3ChatResponse(String answer, List<String> sources, boolean grounded, String ticketNo) {
        this (answer, sources, grounded, ticketNo, List.of());
    }

    // Day 2의 Lab2AnswerDto로부터 응답을 변환하는 정적 팩토리 메서드
    public static Lab3ChatResponse fromRag(Lab2AnswerDto ragDto){
        return new Lab3ChatResponse(
            ragDto.answer(),
            ragDto.sources(),
            ragDto.grounded(),
             null
        );
    }
    //단순 챗봇 텍스트 응답 생성
    public static Lab3ChatResponse of (String answer) {
        return new Lab3ChatResponse(answer,List.of(), false, null, List.of());
    }

    //대화 이력(History) 응답 생성
    public static Lab3ChatResponse ofHistory(List<String> history) {
        return new Lab3ChatResponse("",List.of(), false, null, history);
    }
}
