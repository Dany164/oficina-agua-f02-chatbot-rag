package com.oficinaagua.chatbot.exception;


/** El cliente superó el límite de peticiones; indica cuántos segundos debe esperar. */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;
    public RateLimitExceededException(long retryAfterSeconds) {
        super("Limite de peticiones excedido");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
