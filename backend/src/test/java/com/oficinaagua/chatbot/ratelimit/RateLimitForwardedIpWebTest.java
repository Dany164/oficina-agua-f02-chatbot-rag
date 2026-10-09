package com.oficinaagua.chatbot.ratelimit;

import com.oficinaagua.chatbot.controller.ChatController;
import com.oficinaagua.chatbot.service.rag.RagAnswer;
import com.oficinaagua.chatbot.service.rag.RagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Con {@code chatbot.rate-limit.trust-forwarded-for=true} la clave del límite es el último valor de
 * X-Forwarded-For (el que añadió el proxy de confianza, es decir, el BFF). Como en RateLimitWebTest,
 * el contexto se comparte entre tests, así que cada test usa sus propias IP.
 */
@WebMvcTest(ChatController.class)
@Import(RateLimitConfig.class)
@TestPropertySource(properties = {
        "chatbot.rate-limit.max-requests=2",
        "chatbot.rate-limit.window-seconds=60",
        "chatbot.rate-limit.trust-forwarded-for=true"
})
class RateLimitForwardedIpWebTest {

    private static final String URL = "/api/v1/chat/messages";
    private static final String BODY = "{\"message\":\"hola\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    @BeforeEach
    void setUp() {
        when(ragService.answer(anyString())).thenReturn(new RagAnswer("ok", List.of()));
    }

    private static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    /** connectionIp es la IP del BFF; forwardedFor, el valor de X-Forwarded-For (null = sin cabecera). */
    private ResultActions send(String connectionIp, String forwardedFor) throws Exception {
        var request = post(URL)
                .with(fromIp(connectionIp))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY);
        if (forwardedFor != null) {
            request = request.header("X-Forwarded-For", forwardedFor);
        }
        return mockMvc.perform(request);
    }

    @Test
    void usesTheForwardedIpAsClientKey() throws Exception {
        // Todas las peticiones llegan desde el mismo BFF, pero de clientes distintos.
        send("10.1.0.1", "203.0.113.10").andExpect(status().isOk());
        send("10.1.0.1", "203.0.113.10").andExpect(status().isOk());
        send("10.1.0.1", "203.0.113.10").andExpect(status().isTooManyRequests());

        send("10.1.0.1", "203.0.113.11").andExpect(status().isOk());
    }

    @Test
    void usesTheLastEntryBecauseEarlierOnesCanBeSpoofed() throws Exception {
        send("10.1.0.2", "1.1.1.1, 203.0.113.20").andExpect(status().isOk());
        send("10.1.0.2", "2.2.2.2, 203.0.113.20").andExpect(status().isOk());
        send("10.1.0.2", "3.3.3.3, 203.0.113.20").andExpect(status().isTooManyRequests());

        send("10.1.0.2", "3.3.3.3, 203.0.113.21").andExpect(status().isOk());
    }

    @Test
    void fallsBackToTheConnectionIpWithoutHeader() throws Exception {
        send("10.1.0.3", null).andExpect(status().isOk());
        send("10.1.0.3", null).andExpect(status().isOk());
        send("10.1.0.3", null).andExpect(status().isTooManyRequests());

        send("10.1.0.4", null).andExpect(status().isOk());
    }

    @Test
    void fallsBackToTheConnectionIpWithBlankHeader() throws Exception {
        send("10.1.0.5", "  ").andExpect(status().isOk());
        send("10.1.0.5", "  ").andExpect(status().isOk());
        send("10.1.0.5", "  ").andExpect(status().isTooManyRequests());

        send("10.1.0.6", "  ").andExpect(status().isOk());
    }
}
