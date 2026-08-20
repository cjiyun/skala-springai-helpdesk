package com.skala.lab0.myapp.lab3.advisor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
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
    public String getName(){
        return "RagAdvisor";
    }

    @Override
    public int getOrder() {
        return 300;
    }
    
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain){
        String userText = request.prompt() != null ? request.prompt().getContents() : "";

            //Day2에서 사용한 vectorstore 검색 수행
            List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.builder().query(userText).topK(3).build()
            );

            if (!similarDocs.isEmpty()) {
                String context = similarDocs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));
                
                String augmentedUserText = """
                        아래 제공된 참고 문서를 바탕으로 질문에 답변해 주세요.

                        [참고 문서]
                        %s

                        [사용자 질문]
                        %s
                        """.formatted(context, userText);
                ChatClientRequest updatedRequest = request.mutate()
                        .prompt(new Prompt(augmentedUserText))
                        .build();
                return chain.nextCall(updatedRequest);
            }

            return chain.nextCall(request);
    }
}
