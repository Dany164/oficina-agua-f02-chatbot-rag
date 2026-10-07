package com.oficinaagua.chatbot.service.ingestion;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Convierte un documento de conocimiento (Markdown) en chunks listos para guardar en el
 * vector store: un Document por sección, con sus metadatos.
 */
@Component
public class KnowledgeChunkFactory {
    public static final String VISIBILITY_PUBLIC = "PUBLIC";

    private final MarkdownSectionSplitter splitter;

    public KnowledgeChunkFactory(MarkdownSectionSplitter splitter) {
        this.splitter = splitter;
    }

    public List<Document> createChunks(String documentId, String markdown, String contentHash) {
        List<MarkdownSection> sections = splitter.split(markdown);
        List<Document> chunks = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            MarkdownSection section = sections.get(i);
            String text = section.documentTitle() + " - " + section.heading()
                    + "\n\n" + section.content();
            Map<String, Object> metadata = Map.of(
                    "documentId", documentId,
                    "title", section.documentTitle(),
                    "section", section.heading(),
                    "visibility", VISIBILITY_PUBLIC,
                    "contentHash", contentHash,
                    "chunkIndex", i);
            chunks.add(new Document(text, metadata));

        }
        return chunks;
    }
}
