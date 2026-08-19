package com.skala.lab0.myapp.rag.service;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.skala.lab0.myapp.rag.dto.Lab2IngestResponse;

@Service
public class Lab2IngestService {
  private final VectorStore vectorStore;

  public Lab2IngestService(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  public Lab2IngestResponse ingest(Resource resource, String source) {
    TextReader reader = new TextReader(resource);
    reader.getCustomMetadata().putAll(Map.of("source", source, "version", "1"));

    List<Document> documents = reader.get().stream()
      .map(document -> {
        Map<String, Object> metadata = new HashMap<>(document.getMetadata());
        metadata.put("source", source);
        metadata.put("version", "1");
        return new Document(document.getText(), metadata);
      })
      .toList();

    List<Document> chunks = TokenTextSplitter.builder()
        .withChunkSize(400)
        .withMinChunkSizeChars(200)
        .build()
      .apply(documents);

    vectorStore.delete("source == '" + source + "'");
    vectorStore.add(chunks);
    return new Lab2IngestResponse(source, chunks.size());
  }

}