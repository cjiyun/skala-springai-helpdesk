package com.skala.lab0.myapp.lab3.advisor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

public class RagAdvisor implements CallAdvisor {

    private final VectorStore vectorStore;

    public RagAdvisor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
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
        if (request.prompt() == null || request.prompt().getInstructions() == null) {
            return chain.nextCall(request);
        }

        List<Message> messages = request.prompt().getInstructions();

        // 최신 사용자 질의 추출
        int lastUserIdx = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getMessageType() == MessageType.USER) {
                lastUserIdx = i;
                break;
            }
        }

        if (lastUserIdx == -1) {
            return chain.nextCall(request);
        }

        String userText = messages.get(lastUserIdx).getText();

        // 주문 번호 조회나 환불 접수 등 도구 처리가 필요한 질의는 RAG 주입 없이 즉시 체인 통과
        if (isToolAction(userText)) {
            return chain.nextCall(request);
        }

        // VectorStore 검색 수행
        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.builder().query(userText).topK(3).build()
        );

        if (!similarDocs.isEmpty()) {
            // 문서 조각마다 출처 파일명을 함께 표기
            String context = similarDocs.stream()
                    .map(d -> {
                        String src = d.getMetadata().getOrDefault("source", "unknown").toString();
                        return "[출처: " + src + "]\n" + d.getText();
                    })
                    .collect(Collectors.joining("\n\n"));

            String augmentedUserText = """
                    아래 제공된 참고 문서를 바탕으로 질문에 정확하게 답변해 주세요.
                    답변 작성 시, 참고한 문서의 출처 파일명(예: (출처: return-policy.md))을 답변 문장 끝에 반드시 함께 명시해 주세요.

                    [참고 문서]
                    %s

                    [사용자 질문]
                    %s
                    """.formatted(context, userText);

            List<Message> updatedMessages = new ArrayList<>(messages);
            updatedMessages.set(lastUserIdx, new UserMessage(augmentedUserText));

            ChatClientRequest updatedRequest = request.mutate()
                    .prompt(new Prompt(updatedMessages))
                    .build();

            return chain.nextCall(updatedRequest);
        }

        return chain.nextCall(request);
    }

    private boolean isToolAction(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return lower.contains("ord-") || lower.contains("접수");
    }
}