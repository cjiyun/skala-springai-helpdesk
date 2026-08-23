package com.skala.lab0.myapp.lab3.advisor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;

public class RagAdvisor implements CallAdvisor, StreamAdvisor {

    public static final String RETRIEVED_DOCUMENTS = "helpdesk_retrieved_documents";
    public static final String RAG_ATTEMPTED = "helpdesk_rag_attempted";

    private final VectorStore vectorStore;
    private final int topK;
    private final double threshold;

    public RagAdvisor(VectorStore vectorStore, int topK, double threshold) {
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.threshold = threshold;
    }

    @Override
    public String getName() {
        return "RagAdvisor";
    }

    @Override
    public int getOrder() {
        return 300;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Augmented augmented = augment(request);
        ChatClientResponse response = chain.nextCall(augmented.request());
        return response.mutate()
                .context(RETRIEVED_DOCUMENTS, augmented.documents())
                .context(RAG_ATTEMPTED, augmented.attempted())
                .build();
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        Augmented augmented = augment(request);
        return chain.nextStream(augmented.request()).map(response -> response.mutate()
                .context(RETRIEVED_DOCUMENTS, augmented.documents())
                .context(RAG_ATTEMPTED, augmented.attempted())
                .build());
    }

    private Augmented augment(ChatClientRequest request) {
        if (request.prompt() == null) return new Augmented(request, List.of(), false);
        List<Message> messages = request.prompt().getInstructions();
        int lastUserIdx = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getMessageType() == MessageType.USER) { lastUserIdx = i; break; }
        }
        if (lastUserIdx < 0) return new Augmented(request, List.of(), false);
        String userText = messages.get(lastUserIdx).getText();
        if (isToolOnlyRequest(userText)) return new Augmented(request, List.of(), false);
        List<Document> similarDocs = vectorStore.similaritySearch(SearchRequest.builder()
                .query(searchQuery(messages, lastUserIdx, userText))
                .topK(topK).similarityThreshold(threshold).build());
        if (similarDocs.isEmpty()) return new Augmented(request, List.of(), true);
        String context = similarDocs.stream()
                    .map(d -> {
                        String src = d.getMetadata().getOrDefault("source", "unknown").toString();
                        String version = d.getMetadata().getOrDefault("version", "unknown").toString();
                        return "[출처: " + src + ", 버전: " + version + "]\n" + d.getText();
                    })
                    .collect(Collectors.joining("\n\n"));

            String augmentedUserText = """
                    아래 제공된 참고 문서를 바탕으로 질문에 정확하게 답변해 주세요.
                    질문에 "그거" 같은 대명사가 있으면 대화 이력에서 해석한 주문번호나 상품명을 답변에 반드시 밝혀 주세요.
                    참고 문서가 질문의 답을 뒷받침하지 않으면 다른 설명 없이 NO_EVIDENCE라고만 답하세요.

                    [참고 문서]
                    %s

                    [사용자 질문]
                    %s
                    """.formatted(context, userText);

        List<Message> updatedMessages = new ArrayList<>(messages);
        updatedMessages.set(lastUserIdx, new UserMessage(augmentedUserText));
        return new Augmented(request.mutate().prompt(new Prompt(updatedMessages)).build(), similarDocs, true);
    }

    private boolean isToolOnlyRequest(String text) {
        String lower = text.toLowerCase();
        boolean policy = List.of("규정", "정책", "가능", "돼", "기한", "조건").stream().anyMatch(lower::contains);
        boolean statusFollowUp = lower.contains("어떻게 됐") || lower.contains("진행 상황");
        boolean tool = lower.contains("접수") || statusFollowUp || (lower.matches(".*\\d{5}.*")
                && List.of("주문", "배송", "도착", "어디").stream().anyMatch(lower::contains));
        return tool && !policy;
    }

    private String searchQuery(List<Message> messages, int lastUserIdx, String current) {
        if (!List.of("그거", "그 주문", "그 상품").stream().anyMatch(current::contains)) return current;
        List<String> recent = new ArrayList<>();
        for (int i = lastUserIdx - 1; i >= 0 && recent.size() < 2; i--) {
            if (messages.get(i).getMessageType() == MessageType.USER) recent.add(0, messages.get(i).getText());
        }
        recent.add(current);
        return String.join("\n", recent);
    }

    private record Augmented(ChatClientRequest request, List<Document> documents, boolean attempted) {}
}
