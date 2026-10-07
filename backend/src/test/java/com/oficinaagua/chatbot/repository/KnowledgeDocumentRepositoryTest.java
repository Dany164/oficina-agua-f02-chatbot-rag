package com.oficinaagua.chatbot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.oficinaagua.chatbot.PostgresTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(PostgresTestConfig.class)
class KnowledgeDocumentRepositoryTest {

    @Autowired
    private KnowledgeDocumentRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanTable() {
        jdbcTemplate.update("DELETE FROM knowledge_document");
    }

    @Test
    void findHashReturnsEmptyWhenDocumentWasNeverIngested() {
        assertThat(repository.findHash("03-tarifas.md")).isEmpty();
    }

    @Test
    void upsertInsertsNewDocument() {
        repository.upsert("03-tarifas.md", "hash-1", 3);

        assertThat(repository.findHash("03-tarifas.md")).contains("hash-1");
    }

    @Test
    void upsertUpdatesHashAndChunkCountWhenDocumentAlreadyExists() {
        repository.upsert("03-tarifas.md", "hash-1", 3);
        repository.upsert("03-tarifas.md", "hash-2", 4);

        assertThat(repository.findHash("03-tarifas.md")).contains("hash-2");
        Integer rows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM knowledge_document", Integer.class);
        Integer chunks = jdbcTemplate.queryForObject(
                "SELECT chunk_count FROM knowledge_document WHERE document_id = '03-tarifas.md'",
                Integer.class);
        assertThat(rows).isEqualTo(1);
        assertThat(chunks).isEqualTo(4);
    }
}