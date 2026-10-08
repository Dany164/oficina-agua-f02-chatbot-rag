package com.oficinaagua.chatbot.service.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

@ExtendWith(MockitoExtension.class)
class KnowledgeRetrieverTest {

    @Mock
    private VectorStore vectorStore;

    @Captor
    private ArgumentCaptor<SearchRequest> requestCaptor;

    private KnowledgeRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new KnowledgeRetriever(vectorStore, 4, 0.5);
    }

    @Test
    void searchesWithConfiguredTopKThresholdAndPublicFilter() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        retriever.retrieve("¿Cuál es el horario de atención?");

        verify(vectorStore).similaritySearch(requestCaptor.capture());
        SearchRequest request = requestCaptor.getValue();
        assertThat(request.getQuery()).isEqualTo("¿Cuál es el horario de atención?");
        assertThat(request.getTopK()).isEqualTo(4);
        assertThat(request.getSimilarityThreshold()).isEqualTo(0.5);
        assertThat(request.getFilterExpression())
                .isEqualTo(new FilterExpressionBuilder().eq("visibility", "PUBLIC").build());
    }

    @Test
    void returnsTheChunksFoundByTheVectorStore() {
        Document chunk = new Document("Horario de atención", Map.of("documentId", "01.md", "section", "Horario"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(chunk));

        List<Document> result = retriever.retrieve("horario");

        assertThat(result).containsExactly(chunk);
    }

    @Test
    void returnsEmptyListWhenNothingPassesTheThreshold() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        assertThat(retriever.retrieve("¿Cómo se cocina un pastel?")).isEmpty();
    }
}