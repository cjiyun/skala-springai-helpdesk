package com.skala.helpdesk.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.skala.helpdesk.rag.dto.ChunkResponse;
import com.skala.helpdesk.rag.dto.IngestResponse;
import com.skala.helpdesk.rag.IngestService;
import com.skala.helpdesk.rag.SearchService;

@SpringBootTest
@AutoConfigureMockMvc
class ChunkAdminControllerTest {
  @Autowired
  MockMvc mvc;

  @MockitoBean
  SearchService search;

  @MockitoBean
  IngestService ingest;

  @Test
  void 관리자는_문서를_인제스트한다() throws Exception {
    when(ingest.ingest(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.eq("return-policy.md"), org.mockito.ArgumentMatchers.eq("1")))
        .thenReturn(new IngestResponse("return-policy.md", 1));
    when(ingest.ingest(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.eq("shipping-policy.md"), org.mockito.ArgumentMatchers.eq("1")))
        .thenReturn(new IngestResponse("shipping-policy.md", 1));
    when(ingest.ingest(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.eq("membership.md"), org.mockito.ArgumentMatchers.eq("1")))
        .thenReturn(new IngestResponse("membership.md", 1));

    mvc.perform(post("/api/admin/ingest")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].source").value("return-policy.md"));
  }

  @Test
  void 관리자는_검색된_청크의_출처_버전_점수_미리보기를_확인한다() throws Exception {
    when(search.retrieve("반품", 3)).thenReturn(List.of(
        new ChunkResponse("return-policy.md", "2026-08-21", 0.91, "7일 이내 반품")));

    mvc.perform(get("/api/admin/chunks")
            .param("q", "반품")
            .param("topK", "3")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].source").value("return-policy.md"))
        .andExpect(jsonPath("$[0].version").value("2026-08-21"))
        .andExpect(jsonPath("$[0].score").value(0.91))
        .andExpect(jsonPath("$[0].preview").value("7일 이내 반품"));
    verify(search).retrieve("반품", 3);
  }
}
