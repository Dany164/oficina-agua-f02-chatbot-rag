package com.oficinaagua.chatbot.repository;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Registro de los documentos de conocimiento ya ingeridos (tabla knowledge_document).
 */
@Repository
public class KnowledgeDocumentRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeDocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Hash guardado para el documento, o vacío si nunca fue ingerido. */
    public Optional<String> findHash(String documentId) {
        return jdbcTemplate
                .queryForList("SELECT content_hash FROM knowledge_document WHERE document_id = ?",
                        String.class, documentId)
                .stream()
                .findFirst();
    }

    /** Inserta el registro del documento o, si ya existía, actualiza su hash y su fecha. */
    public void upsert(String documentId, String contentHash, int chunkCount) {
        jdbcTemplate.update("""
                INSERT INTO knowledge_document (document_id, content_hash, chunk_count)
                VALUES (?, ?, ?)
                ON CONFLICT (document_id) DO UPDATE
                SET content_hash = EXCLUDED.content_hash,
                    chunk_count = EXCLUDED.chunk_count,
                    ingested_at = now()
                """, documentId, contentHash, chunkCount);
    }
}