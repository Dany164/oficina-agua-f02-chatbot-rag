package com.oficinaagua.chatbot.service.rag;

import java.util.List;

/** Resultado del RAG: la respuesta y las fuentes de los fragmentos usados. */
public record RagAnswer(String answer, List<RagSource> sources) {
}