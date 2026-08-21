package com.skala.lab0.myapp.lab3.chat;

import java.util.List;

public record AnswerDto(String answer, List<Source> sources, boolean toolUsed) {
  public static AnswerDto of(String answer) {
    return new AnswerDto(answer, List.of(), false);
  }
}
