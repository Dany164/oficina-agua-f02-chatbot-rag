package com.oficinaagua.chatbot.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class KnowledgeChunkFactoryTest {

    private final KnowledgeChunkFactory factory =
            new KnowledgeChunkFactory(new MarkdownSectionSplitter());

    private static final String MD = """
            # Tarifas
            > Documento ficticio.

            ## Residencial
            Q 3.50 por m3 extra.

            ## Comercial
            Q 5.00 por m3 extra.
            """;

    @Test
    void createsOneChunkPerSection() {
        List<Document> chunks = factory.createChunks("03-tarifas.md", MD, "abc123");

        assertThat(chunks).hasSize(2);
    }

    @Test
    void chunkTextIncludesTitleAndSectionHeading() {
        List<Document> chunks = factory.createChunks("03-tarifas.md", MD, "abc123");

        assertThat(chunks.get(0).getText())
                .startsWith("Tarifas - Residencial")
                .contains("Q 3.50 por m3 extra.");
    }

    @Test
    void chunkMetadataDescribesItsOrigin() {
        List<Document> chunks = factory.createChunks("03-tarifas.md", MD, "abc123");

        assertThat(chunks.get(1).getMetadata())
                .containsEntry("documentId", "03-tarifas.md")
                .containsEntry("title", "Tarifas")
                .containsEntry("section", "Comercial")
                .containsEntry("visibility", "PUBLIC")
                .containsEntry("contentHash", "abc123")
                .containsEntry("chunkIndex", 1);
    }
}