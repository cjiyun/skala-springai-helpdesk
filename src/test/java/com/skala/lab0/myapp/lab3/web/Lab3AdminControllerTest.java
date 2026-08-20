package com.skala.lab0.myapp.lab3.web;

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

import com.skala.lab0.myapp.lab3.ticket.TicketService;
import com.skala.lab0.myapp.lab3.ticket.TicketView;
import com.skala.lab0.myapp.lab3.ticket.TicketAlreadyApprovedException;
import com.skala.lab0.myapp.lab3.ticket.TicketNotFoundException;

@SpringBootTest
@AutoConfigureMockMvc
class Lab3AdminControllerTest {
  @Autowired
  MockMvc mvc;

  @MockitoBean
  TicketService tickets;

  @Test
  void 관리자는_PENDING_티켓을_조회한다() throws Exception {
    when(tickets.pending()).thenReturn(List.of(
        new TicketView("T1", "12345", "PENDING", "승인 대기 중입니다.")));

    mvc.perform(get("/lab3/admin/tickets/pending")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ticketNo").value("T1"))
        .andExpect(jsonPath("$[0].status").value("PENDING"));
  }

  @Test
  void 관리자는_별도_API로_티켓을_승인한다() throws Exception {
    when(tickets.approve("T1"))
        .thenReturn(new TicketView("T1", "12345", "APPROVED", "승인되었습니다."));

    mvc.perform(post("/lab3/admin/tickets/T1/approve")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"));
    verify(tickets).approve("T1");
  }

  @Test
  void 일반_사용자는_티켓을_승인할_수_없다() throws Exception {
    mvc.perform(post("/lab3/admin/tickets/T1/approve")
            .with(user("user1").roles("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void 존재하지_않는_티켓_승인은_404를_반환한다() throws Exception {
    when(tickets.approve("missing")).thenThrow(new TicketNotFoundException("missing"));

    mvc.perform(post("/lab3/admin/tickets/missing/approve")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isNotFound());
  }

  @Test
  void 이미_승인된_티켓의_재승인은_409를_반환한다() throws Exception {
    when(tickets.approve("T1")).thenThrow(new TicketAlreadyApprovedException());

    mvc.perform(post("/lab3/admin/tickets/T1/approve")
            .with(user("admin").roles("ADMIN")))
        .andExpect(status().isConflict());
  }
}
