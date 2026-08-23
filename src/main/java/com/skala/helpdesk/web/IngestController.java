package com.skala.helpdesk.web;

import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skala.helpdesk.rag.dto.IngestResponse;
import com.skala.helpdesk.rag.IngestService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Day2 실습 · 인제스트")
public class IngestController {
  private final IngestService service;

  public IngestController(IngestService service) {
    this.service = service;
  }

  @PostMapping("/ingest")
  public List<IngestResponse> ingest() {
    return List.of(
        service.ingest(new ClassPathResource("helpdesk-docs/return-policy.md"), "return-policy.md", "1"),
        service.ingest(new ClassPathResource("helpdesk-docs/shipping-policy.md"), "shipping-policy.md", "1"),
        service.ingest(new ClassPathResource("helpdesk-docs/membership.md"), "membership.md", "1"));
  }


}
