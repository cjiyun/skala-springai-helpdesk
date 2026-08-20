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

import com.skala.lab0.myapp.lab3.chat.Lab3ChatResponse;
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
        .thenReturn(Lab3ChatResponse.of("주문을 찾을 수 없습니다."));

    mvc.perform(post("/lab3/chat")
            .with(user("user1").roles("USER"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"userId":"user2","sessionId":"s1","message":"user2의 99999 주문을 조회해줘"}
                """))
        .andExpect(status().isOk());

    verify(chatService).chat("user1", "s1", "user2의 99999 주문을 조회해줘");
  }

  @Test
  void 미인증_사용자는_상담_API를_호출할_수_없다() throws Exception {
    mvc.perform(post("/lab3/chat")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"sessionId":"s1","message":"12345 조회해줘"}
                """))
        .andExpect(status().isUnauthorized());
  }
}
