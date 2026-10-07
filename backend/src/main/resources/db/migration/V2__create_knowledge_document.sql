CREATE TABLE knowledge_document (
                                    document_id  text        PRIMARY KEY,
                                    content_hash text        NOT NULL,
                                    chunk_count  integer     NOT NULL,
                                    ingested_at  timestamptz NOT NULL DEFAULT now()
);