package com.oficinaagua.chatbot.service.rag;

/** Genera la respuesta a partir del prompt. Aísla al servicio del proveedor de LLM. */
public interface AnswerGenerator {

    /** Texto de la respuesta; puede ser nulo o vacío si el modelo no devolvió nada. */
    String generate(RagPrompt prompt);
}