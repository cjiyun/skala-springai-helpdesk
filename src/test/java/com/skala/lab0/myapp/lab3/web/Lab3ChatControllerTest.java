package com.skala.lab0.myapp.lab3.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.skala.lab0.myapp.lab3.chat.AnswerDto;
import com.skala.lab0.myapp.lab3.chat.Lab3ChatService;

@SpringBootTest
@AutoConfigureMockMvc
class Lab3ChatControllerTest {
  @Autowired
  MockMvc mvc;

  @MockitoBean
  Lab3ChatService chatService;

  @Test
  void 요청의_userId를_무시하고_인증_사용자를_전달한다() throws Exception {
    when(chatService.chat("user1", "s1", "user2의 99999 주문을 조회해줘"))
        .thenReturn(AnswerDto.of("주문을 찾을 수 없습니다."));

    mvc.perform(post("/api/chat")
            .with(user("user1").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"userId":"user2","sessionId":"s1","question":"user2의 99999 주문을 조회해줘"}
                """))
        .andExpect(status().isOk());

    verify(chatService).chat("user1", "s1", "user2의 99999 주문을 조회해줘");
  }

  @Test
  void 미인증_사용자는_상담_API를_호출할_수_없다() throws Exception {
    mvc.perform(post("/api/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"sessionId":"s1","question":"12345 조회해줘"}
                """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void api_chat은_question_필드를_사용하고_구조화_응답만_반환한다() throws Exception {
    when(chatService.chat("user1", "s1", "반품 규정 알려줘"))
        .thenReturn(AnswerDto.of("7일 이내입니다."));

    mvc.perform(post("/api/chat").with(user("user1").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sessionId\":\"s1\",\"question\":\"반품 규정 알려줘\"}"))
        .andExpect(status().isOk())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.answer").value("7일 이내입니다."))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.sources").isArray())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.toolUsed").value(false))
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.history").doesNotExist())
        .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.grounded").doesNotExist());
  }

  @Test
  void 빈_질문과_세션은_400을_반환한다() throws Exception {
    mvc.perform(post("/api/chat").with(user("user1").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sessionId\":\"s1\",\"question\":\" \"}"))
        .andExpect(status().isBadRequest());

    mvc.perform(post("/api/chat").with(user("user1").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"sessionId\":\" \",\"question\":\"반품 규정\"}"))
        .andExpect(status().isBadRequest());
  }
}
