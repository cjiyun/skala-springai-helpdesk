package com.skala.lab0.myapp.rag.web;

import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skala.lab0.myapp.rag.dto.Lab2IngestResponse;
import com.skala.lab0.myapp.rag.service.Lab2IngestService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/lab2")
@Tag(name = "Day2 실습 · 인제스트")
public class Lab2IngestController {
  private final Lab2IngestService service;

  public Lab2IngestController(Lab2IngestService service) {
    this.service = service;
  }

  @PostMapping("/ingest")
  public List<Lab2IngestResponse> ingest() {
    return List.of(
        service.ingest(new ClassPathResource("lab2-docs/return-policy.md"), "return-policy"),
        service.ingest(new ClassPathResource("lab2-docs/shipping-policy.md"), "shipping-policy"),
        service.ingest(new ClassPathResource("lab2-docs/membership.md"), "membership"));
  }


}