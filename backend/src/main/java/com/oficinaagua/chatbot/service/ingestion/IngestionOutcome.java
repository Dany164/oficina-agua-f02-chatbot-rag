package com.oficinaagua.chatbot.service.ingestion;

/** Resultado de procesar un documento de conocimiento. */
public enum IngestionOutcome {
    /** El documento era nuevo o cambió: se (re)generaron sus chunks. */
    INGESTED,
    /** El contenido no cambió desde la última ingesta: no se hizo nada. */
    UNCHANGED
}