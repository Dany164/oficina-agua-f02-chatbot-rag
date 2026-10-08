package com.oficinaagua.chatbot.exception;

/** El proveedor de LLM o la base vectorial no están disponibles (se traduce a HTTP 503). */

public class AiServiceUnavailableException extends RuntimeException {
    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
