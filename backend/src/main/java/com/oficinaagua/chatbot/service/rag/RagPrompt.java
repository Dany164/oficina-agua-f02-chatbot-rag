package com.oficinaagua.chatbot.service.rag;

/**
 * Mensajes que se envían al LLM: las reglas con el contexto, y la pregunta del usuario.
 */
public record RagPrompt(String systemMessage, String userMessage) {
}
