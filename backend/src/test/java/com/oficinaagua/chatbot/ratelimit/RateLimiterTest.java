package com.oficinaagua.chatbot.ratelimit;

import com.oficinaagua.chatbot.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterTest {

    /** Reloj controlado a mano: el test avanza el tiempo sin esperar. */
    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private MutableClock clock;
    private RateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        limiter = new RateLimiter(3, Duration.ofSeconds(60), clock);
    }

    @Test
    void allowsRequestsUpToTheLimit() {
        assertThatCode(() -> {
            limiter.check("1.1.1.1");
            limiter.check("1.1.1.1");
            limiter.check("1.1.1.1");
        }).doesNotThrowAnyException();
    }

    @Test
    void rejectsRequestOverTheLimitWithRetryAfter() {
        for (int i = 0; i < 3; i++) {
            limiter.check("1.1.1.1");
        }

        assertThatThrownBy(() -> limiter.check("1.1.1.1"))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> assertThat(((RateLimitExceededException) ex).getRetryAfterSeconds())
                        .isEqualTo(60));
    }

    @Test
    void retryAfterShrinksAsTheWindowAdvances() {
        for (int i = 0; i < 3; i++) {
            limiter.check("1.1.1.1");
        }
        clock.advance(Duration.ofSeconds(20));

        assertThatThrownBy(() -> limiter.check("1.1.1.1"))
                .satisfies(ex -> assertThat(((RateLimitExceededException) ex).getRetryAfterSeconds())
                        .isEqualTo(40));
    }

    @Test
    void clientsAreLimitedIndependently() {
        for (int i = 0; i < 3; i++) {
            limiter.check("1.1.1.1");
        }

        assertThatCode(() -> limiter.check("2.2.2.2")).doesNotThrowAnyException();
    }

    @Test
    void allowsRequestsAgainAfterTheWindowExpires() {
        for (int i = 0; i < 3; i++) {
            limiter.check("1.1.1.1");
        }
        clock.advance(Duration.ofSeconds(60));

        assertThatCode(() -> limiter.check("1.1.1.1")).doesNotThrowAnyException();
    }
}