package com.skala.helpdesk.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.helpdesk.rag.dto.ChunkResponse;
import com.skala.helpdesk.rag.SearchService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/lab2")
@Tag(name = "Day2 실습 · 문서 검색")
public class SearchController {
  private final SearchService service;

  public SearchController(SearchService service) {
    this.service = service;
  }

  @GetMapping("/retrieve")
  public List<ChunkResponse> retrieve(
      @RequestParam String q,
      @RequestParam(defaultValue = "4") int topK) {
    return service.retrieve(q, topK);
  }
}