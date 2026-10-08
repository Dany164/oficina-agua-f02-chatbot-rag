package com.oficinaagua.chatbot.service.rag;

import com.oficinaagua.chatbot.service.ingestion.KnowledgeChunkFactory;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Búsqueda semántica de los fragmentos de conocimiento más relevantes para una pregunta.
 * Solo devuelve información pública (visibility = PUBLIC).
 */
@Service
public class KnowledgeRetriever {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRetriever.class);

    private final VectorStore vectorStore;
    private final int topK;
    private final double similarityThreshold;

    public KnowledgeRetriever(VectorStore vectorStore,
                              @Value("${chatbot.rag.top-k}") int topK,
                              @Value("${chatbot.rag.similarity-threshold}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    /** Fragmentos más parecidos a la pregunta, de mayor a menor similitud; vacío si ninguno supera el umbral. */
    public List<Document> retrieve(String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .filterExpression(new FilterExpressionBuilder()
                        .eq("visibility", KnowledgeChunkFactory.VISIBILITY_PUBLIC)
                        .build())
                .build();

        List<Document> results = vectorStore.similaritySearch(request);

        // Se registra el score (no la pregunta) para poder calibrar el umbral con datos reales.
        results.forEach(doc -> log.info("Fragmento recuperado: {} / {} (score {})",
                doc.getMetadata().get("documentId"), doc.getMetadata().get("section"), doc.getScore()));
        log.info("Búsqueda: {} fragmentos sobre el umbral {}", results.size(), similarityThreshold);
        return results;
    }
}
