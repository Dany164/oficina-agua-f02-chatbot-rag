package com.oficinaagua.chatbot.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.Duration;

/** Crea el limitador con la configuración y lo registra solo sobre los endpoints del chat. */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimiter rateLimiter;

    public RateLimitConfig(
            @Value("${chatbot.rate-limit.max-requests}") int maxRequests,
            @Value("${chatbot.rate-limit.window-seconds}") long windowSeconds) {
        this.rateLimiter = new RateLimiter(maxRequests, Duration.ofSeconds(windowSeconds), Clock.systemUTC());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(rateLimiter))
                .addPathPatterns("/api/v1/chat/**");
    }
}
