package com.oficinaagua.chatbot.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.oficinaagua.chatbot.repository.KnowledgeDocumentRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

@ExtendWith(MockitoExtension.class)
class KnowledgeIngestionServiceTest {

    private static final String MD = """
            # Tarifas
            > Documento ficticio.

            ## Residencial
            Q 3.50 por m3 extra.

            ## Comercial
            Q 5.00 por m3 extra.
            """;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Captor
    private ArgumentCaptor<List<Document>> chunksCaptor;

    @Captor
    private ArgumentCaptor<String> hashCaptor;

    private KnowledgeIngestionService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeIngestionService(
                new KnowledgeChunkFactory(new MarkdownSectionSplitter()),
                documentRepository,
                vectorStore);
    }

    @Test
    void ingestsNewDocument() {
        IngestionOutcome outcome = service.ingestDocument("03-tarifas.md", MD);

        assertThat(outcome).isEqualTo(IngestionOutcome.INGESTED);
        verify(vectorStore).delete(any(Filter.Expression.class));
        verify(vectorStore).add(chunksCaptor.capture());
        assertThat(chunksCaptor.getValue()).hasSize(2);
        verify(documentRepository).upsert(eq("03-tarifas.md"), anyString(), eq(2));
    }

    @Test
    void skipsDocumentWhoseContentDidNotChange() {
        service.ingestDocument("03-tarifas.md", MD);
        verify(documentRepository).upsert(eq("03-tarifas.md"), hashCaptor.capture(), anyInt());
        when(documentRepository.findHash("03-tarifas.md"))
                .thenReturn(Optional.of(hashCaptor.getValue()));

        IngestionOutcome second = service.ingestDocument("03-tarifas.md", MD);

        assertThat(second).isEqualTo(IngestionOutcome.UNCHANGED);
        verify(vectorStore, times(1)).add(anyList());
        verify(vectorStore, times(1)).delete(any(Filter.Expression.class));
    }

    @Test
    void reingestsDocumentWhenContentChanged() {
        when(documentRepository.findHash("03-tarifas.md")).thenReturn(Optional.of("hash-viejo"));

        IngestionOutcome outcome = service.ingestDocument("03-tarifas.md", MD);

        assertThat(outcome).isEqualTo(IngestionOutcome.INGESTED);
        verify(vectorStore).delete(any(Filter.Expression.class));
        verify(vectorStore).add(anyList());
    }

    @Test
    void ingestsOnlyMarkdownFilesFromDirectory(@TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("b.md"), MD);
        Files.writeString(directory.resolve("a.md"), MD);
        Files.writeString(directory.resolve("notas.txt"), "no es markdown");

        Map<String, IngestionOutcome> results = service.ingestDirectory(directory);

        assertThat(results.keySet()).containsExactly("a.md", "b.md");
        verify(vectorStore, times(2)).add(anyList());
    }
}