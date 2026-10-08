package com.oficinaagua.chatbot.service.rag;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

/**
 * Flujo de consulta del RAG: recuperar -> (si no hay nada, responder sin LLM) -> prompt -> generar.
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final KnowledgeRetriever retriever;
    private final RagPromptBuilder promptBuilder;
    private final AnswerGenerator answerGenerator;

    public RagService(KnowledgeRetriever retriever, RagPromptBuilder promptBuilder,
                      AnswerGenerator answerGenerator) {
        this.retriever = retriever;
        this.promptBuilder = promptBuilder;
        this.answerGenerator = answerGenerator;
    }

    public RagAnswer answer(String question) {
        List<Document> chunks = retriever.retrieve(question);
        if (chunks.isEmpty()) {
            log.info("Sin fragmentos relevantes: se responde sin llamar al LLM");
            return noInformation();
        }

        String answer = answerGenerator.generate(promptBuilder.build(question, chunks));
        if (answer == null || answer.isBlank()) {
            log.warn("El LLM no devolvió texto: se responde que no hay información");
            return noInformation();
        }
        if (answer.contains(RagPromptBuilder.NO_INFORMATION_ANSWER)) {
            return new RagAnswer(answer.strip(), List.of()); // no se citan fuentes si no se pudo responder
        }
        return new RagAnswer(answer.strip(), toSources(chunks));
    }

    private RagAnswer noInformation() {
        return new RagAnswer(RagPromptBuilder.NO_INFORMATION_ANSWER, List.of());
    }

    private List<RagSource> toSources(List<Document> chunks) {
        return chunks.stream()
                .map(chunk -> new RagSource(sourceTitle(chunk), metadata(chunk, "documentId")))
                .toList();
    }

    private String sourceTitle(Document chunk) {
        String title = metadata(chunk, "title");
        String section = metadata(chunk, "section");
        if (title.isEmpty()) {
            return section;
        }
        return section.isEmpty() ? title : title + " - " + section;
    }

    private String metadata(Document chunk, String key) {
        Object value = chunk.getMetadata().get(key);
        return value == null ? "" : value.toString();
    }
}