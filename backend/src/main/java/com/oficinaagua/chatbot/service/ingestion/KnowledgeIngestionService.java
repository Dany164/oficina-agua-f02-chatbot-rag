package com.oficinaagua.chatbot.service.ingestion;

import com.oficinaagua.chatbot.repository.KnowledgeDocumentRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

/**
 * Ingesta de documentos de conocimiento: Markdown -> chunks -> embeddings -> pgvector.
 * Es idempotente: un documento sin cambios no se vuelve a procesar.
 */

@Service
public class KnowledgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final KnowledgeChunkFactory chunkFactory;
    private final KnowledgeDocumentRepository documentRepository;
    private final VectorStore vectorStore;

    public KnowledgeIngestionService(KnowledgeChunkFactory chunkFactory,
                                     KnowledgeDocumentRepository documentRepository,
                                     VectorStore vectorStore) {
        this.chunkFactory = chunkFactory;
        this.documentRepository = documentRepository;
        this.vectorStore = vectorStore;
    }

    /** Procesa todos los archivos .md de la carpeta, en orden alfabético. */
    public Map<String, IngestionOutcome> ingestDirectory(Path directory) {
        Map<String, IngestionOutcome> results = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(directory)) {
            List<Path> markdownFiles = files
                    .filter(file -> file.getFileName().toString().endsWith(".md"))
                    .sorted()
                    .toList();
            for (Path file : markdownFiles) {
                String documentId = file.getFileName().toString();
                String markdown = Files.readString(file, StandardCharsets.UTF_8);
                results.put(documentId, ingestDocument(documentId, markdown));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "No se pudo leer la carpeta de conocimiento: " + directory, e);
        }
        return results;
    }

    /** Procesa un documento: lo omite si no cambió; si cambió, reemplaza sus chunks. */
    public IngestionOutcome ingestDocument(String documentId, String markdown) {
        String hash = sha256(markdown);
        if (documentRepository.findHash(documentId).filter(hash::equals).isPresent()) {
            log.info("Documento sin cambios, se omite: {}", documentId);
            return IngestionOutcome.UNCHANGED;
        }

        List<Document> chunks = chunkFactory.createChunks(documentId, markdown, hash);
        vectorStore.delete(new FilterExpressionBuilder().eq("documentId", documentId).build());
        if (chunks.isEmpty()) {
            log.warn("El documento {} no tiene secciones '##'; no se generaron chunks", documentId);
        } else {
            vectorStore.add(chunks);
        }
        documentRepository.upsert(documentId, hash, chunks.size());

        log.info("Documento ingerido: {} ({} chunks)", documentId, chunks.size());
        return IngestionOutcome.INGESTED;
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = text.replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
        }
    }
}