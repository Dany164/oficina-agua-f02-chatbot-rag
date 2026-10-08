package com.oficinaagua.chatbot.service.rag;

/** Fuente citada en una respuesta: de qué documento y sección salió la información. */
public record RagSource (String title, String documentId) {
}
