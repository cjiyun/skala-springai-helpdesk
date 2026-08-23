package com.skala.helpdesk.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebUiTest {
  @Autowired
  MockMvc mvc;

  @Test
  void 루트에서_인증_없이_최소_채팅_UI를_제공한다() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk());
    String html = mvc.perform(get("/index.html"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

    assertThat(html).contains("SKALA HelpDesk AI", "id=\"session\"", "id=\"question\"",
        "id=\"new-session\"", "id=\"load-history\"", "id=\"delete-session\"",
        "id=\"login\"", "id=\"logout\"", "id=\"history\"", "role=\"status\"", "src=\"/app.js\"")
        .contains("type=\"hidden\" id=\"session\"", "id=\"auth-status\"")
        .doesNotContain("<label>세션 ID", "id=\"history-status\"");
  }

  @Test
  void UI는_POST_SSE와_마지막_sources_이벤트를_처리한다() throws Exception {
    String script = mvc.perform(get("/app.js"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

    assertThat(script).contains("fetch('/api/chat/stream'", "method: 'POST'",
        "name === 'token'", "name === 'sources'", "activeRequest?.abort()",
        "data.push(line.slice(5))")
        .doesNotContain("data.push(line.slice(5).trimStart())");
  }

  @Test
  void UI에서_인증된_사용자의_세션별_대화_이력을_조회한다() throws Exception {
    String script = mvc.perform(get("/app.js"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

    assertThat(script).contains("fetch(`/api/chat/history?sessionId=${encodeURIComponent(sessionId)}`",
        "fetch('/api/auth/login'", "fetch('/api/auth/logout'", "showHistory", "method: 'DELETE'",
        "localStorage.setItem", "createSessionId()", "className = 'tool-card'")
        .doesNotContain("web-session-1", "localStorage.setItem('password'", "authorization()");
  }

  @Test
  void 데스크톱에서는_대화_영역만_독립적으로_스크롤한다() throws Exception {
    String style = mvc.perform(get("/style.css"))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

    assertThat(style).contains(".app-shell {", "height: 100vh", "overflow: hidden",
        ".conversation {", "overflow-y: auto");
  }
}
