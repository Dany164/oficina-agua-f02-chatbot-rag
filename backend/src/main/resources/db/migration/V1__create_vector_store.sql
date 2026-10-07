CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE vector_store (
                              id        uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                              content   text NOT NULL,
                              metadata  json NOT NULL DEFAULT '{}'::json,
                              embedding vector(1024) NOT NULL
);

CREATE INDEX vector_store_embedding_idx
    ON vector_store USING hnsw (embedding vector_cosine_ops);