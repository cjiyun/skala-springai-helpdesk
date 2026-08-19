package com.skala.lab0.myapp.rag.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.lab0.myapp.rag.dto.Lab2ChunkResponse;
import com.skala.lab0.myapp.rag.service.Lab2SearchService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/lab2")
@Tag(name = "Day2 실습 · 문서 검색")
public class Lab2SearchController {
  private final Lab2SearchService service;

  public Lab2SearchController(Lab2SearchService service) {
    this.service = service;
  }

  @GetMapping("/retrieve")
  public List<Lab2ChunkResponse> retrieve(
      @RequestParam String q,
      @RequestParam(defaultValue = "4") int topK) {
    return service.retrieve(q, topK);
  }
}