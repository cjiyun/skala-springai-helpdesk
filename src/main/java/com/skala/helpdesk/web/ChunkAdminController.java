package com.skala.helpdesk.web;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.skala.helpdesk.rag.dto.ChunkResponse;
import com.skala.helpdesk.rag.SearchService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class ChunkAdminController {
  private final SearchService search;

  public ChunkAdminController(SearchService search) {
    this.search = search;
  }

  @GetMapping("/chunks")
  public List<ChunkResponse> chunks(@RequestParam String q,
      @RequestParam(defaultValue = "5") int topK) {
    return search.retrieve(q, topK);
  }
}
