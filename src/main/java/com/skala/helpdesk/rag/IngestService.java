package com.skala.helpdesk.rag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.skala.helpdesk.rag.dto.IngestResponse;

@Service
public class IngestService {
  private final VectorStore vectorStore;

  @Value("${helpdesk.rag.chunk-size:400}")
  private int chunkSize = 400;

  public IngestService(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  public IngestResponse ingest(Resource resource, String source, String version) {
    TextReader reader = new TextReader(resource);
    reader.getCustomMetadata().put("version", version);

    List<Document> documents = reader.get().stream()
      .map(document -> {
        Map<String, Object> metadata = new HashMap<>(document.getMetadata());
        metadata.put("source", source);
        metadata.put("title", title(document.getText(), source));
        metadata.put("docType", "POLICY");
        metadata.put("dept", "HELPDESK");
        return new Document(document.getText(), metadata);
      })
      .toList();

    List<Document> chunks = TokenTextSplitter.builder()
        .withChunkSize(chunkSize)
        .withMinChunkSizeChars(200)
        .build()
      .apply(documents);

    vectorStore.delete("source == '" + source + "'");
    vectorStore.add(chunks);
    return new IngestResponse(source, chunks.size());
  }

  private String title(String text, String fallback) {
    return text.lines().filter(line -> line.startsWith("# ")).findFirst()
        .map(line -> line.substring(2).trim()).orElse(fallback);
  }

}
