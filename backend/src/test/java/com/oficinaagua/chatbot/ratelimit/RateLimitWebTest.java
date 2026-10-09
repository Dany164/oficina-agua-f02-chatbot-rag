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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba el límite de peticiones a través de MVC real. El contexto (y por tanto el contador)
 * se comparte entre tests, así que cada test usa su propia IP.
 */
@WebMvcTest(ChatController.class)
@Import(RateLimitConfig.class)
@TestPropertySource(properties = {
        "chatbot.rate-limit.max-requests=2",
        "chatbot.rate-limit.window-seconds=60"
})
class RateLimitWebTest {

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

    private org.springframework.test.web.servlet.ResultActions send(String ip) throws Exception {
        return mockMvc.perform(post(URL)
                .with(fromIp(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY));
    }

    @Test
    void allowsRequestsWithinTheLimit() throws Exception {
        send("10.0.0.1").andExpect(status().isOk());
        send("10.0.0.1").andExpect(status().isOk());
    }

    @Test
    void returns429WithRetryAfterWhenLimitIsExceeded() throws Exception {
        send("10.0.0.2").andExpect(status().isOk());
        send("10.0.0.2").andExpect(status().isOk());

        send("10.0.0.2")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Demasiadas solicitudes. Intenta de nuevo en unos segundos."));
    }

    @Test
    void otherClientIsNotAffectedByAnotherClientsLimit() throws Exception {
        send("10.0.0.3").andExpect(status().isOk());
        send("10.0.0.3").andExpect(status().isOk());
        send("10.0.0.3").andExpect(status().isTooManyRequests());

        send("10.0.0.4").andExpect(status().isOk());
    }

    @Test
    void ignoresTheForwardedHeaderWhenNotEnabled() throws Exception {
        // Sin la propiedad, X-Forwarded-For se ignora: la clave sigue siendo la IP de la conexión.
        sendForwarded("10.0.0.5", "203.0.113.1").andExpect(status().isOk());
        sendForwarded("10.0.0.5", "203.0.113.2").andExpect(status().isOk());
        sendForwarded("10.0.0.5", "203.0.113.3").andExpect(status().isTooManyRequests());
    }

    private org.springframework.test.web.servlet.ResultActions sendForwarded(String ip, String forwardedFor)
            throws Exception {
        return mockMvc.perform(post(URL)
                .with(fromIp(ip))
                .header("X-Forwarded-For", forwardedFor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY));
    }
}