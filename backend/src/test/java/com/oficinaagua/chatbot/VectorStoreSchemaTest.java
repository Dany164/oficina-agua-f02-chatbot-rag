package com.oficinaagua.chatbot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgresTestConfig.class)
class VectorStoreSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void vectorExtensionIsInstalled() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_extension WHERE extname = 'vector'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void embeddingColumnHasDimension1024() {
        String type = jdbcTemplate.queryForObject("""
                SELECT format_type(a.atttypid, a.atttypmod)
                FROM pg_attribute a
                WHERE a.attrelid = 'vector_store'::regclass AND a.attname = 'embedding'
                """, String.class);
        assertThat(type).isEqualTo("vector(1024)");
    }
}