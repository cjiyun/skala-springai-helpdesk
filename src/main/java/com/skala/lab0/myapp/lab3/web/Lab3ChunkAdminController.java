package com.skala.lab0.myapp.lab3.web;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.skala.lab0.myapp.rag.dto.Lab2ChunkResponse;
import com.skala.lab0.myapp.rag.service.Lab2SearchService;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class Lab3ChunkAdminController {
  private final Lab2SearchService search;

  public Lab3ChunkAdminController(Lab2SearchService search) {
    this.search = search;
  }

  @GetMapping("/chunks")
  public List<Lab2ChunkResponse> chunks(@RequestParam String q,
      @RequestParam(defaultValue = "5") int topK) {
    return search.retrieve(q, topK);
  }
}
