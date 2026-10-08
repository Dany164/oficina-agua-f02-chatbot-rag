package com.oficinaagua.chatbot.controller;

import com.oficinaagua.chatbot.service.rag.RagAnswer;
import com.oficinaagua.chatbot.service.rag.RagService;
import com.oficinaagua.chatbot.service.rag.RagSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.oficinaagua.chatbot.exception.AiServiceUnavailableException;


import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    private static final String URL = "/api/v1/chat/messages";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    @Test
    void validMessage_returns200WithAnswerAndSources() throws Exception {
        when(ragService.answer("Como pago mi factura?")).thenReturn(new RagAnswer(
                "Puede pagar en ventanilla.",
                List.of(new RagSource("Pagos - Medios de pago", "02-pagos"))));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Como pago mi factura?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Puede pagar en ventanilla."))
                .andExpect(jsonPath("$.sources.length()").value(1))
                .andExpect(jsonPath("$.sources[0].title").value("Pagos - Medios de pago"))
                .andExpect(jsonPath("$.sources[0].documentId").value("02-pagos"));

        verify(ragService).answer("Como pago mi factura?");
    }

    @Test
    void answerWithoutSources_returns200WithEmptySources() throws Exception {
        when(ragService.answer(anyString())).thenReturn(new RagAnswer(
                "No tengo información suficiente para responder a esa pregunta.",
                List.of()));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Quien gano el mundial?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isNotEmpty())
                .andExpect(jsonPath("$.sources").isEmpty());
    }

    @Test
    void messageOfMaxLength_returns200() throws Exception {
        when(ragService.answer(anyString())).thenReturn(new RagAnswer("ok", List.of()));

        String body = "{\"message\":\"" + "a".repeat(500) + "\"}";
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void emptyMessage_returns400WithErrorResponse() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.details[0]").value(containsString("message")));
    }

    @Test
    void missingMessage_returns400WithErrorResponse() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(containsString("message")));
    }

    @Test
    void messageTooLong_returns400WithErrorResponse() throws Exception {
        String body = "{\"message\":\"" + "a".repeat(501) + "\"}";
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(containsString("message")));
    }

    @Test
    void malformedJson_returns400WithErrorResponse() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void unsupportedMediaType_returns415WithErrorResponse() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void wrongHttpMethod_returns405WithErrorResponse() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void aiServiceDown_returns503WithGenericErrorResponse() throws Exception {
        when(ragService.answer(anyString()))
                .thenThrow(new AiServiceUnavailableException("ollama caido", new RuntimeException("detalle interno")));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hola\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value(
                        "El servicio no esta disponible temporalmente. Intenta de nuevo mas tarde."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void unexpectedError_returns500WithoutInternalDetails() throws Exception {
        when(ragService.answer(anyString())).thenThrow(new IllegalStateException("detalle secreto"));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hola\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Ocurrio un error interno."))
                .andExpect(content().string(not(containsString("detalle secreto"))));
    }

    @Test
    void blankMessage_returns400WithErrorResponse() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details[0]").value(containsString("message")));
    }

    @Test
    void messageWithLineBreaks_returns200() throws Exception {
        when(ragService.answer(anyString())).thenReturn(new RagAnswer("ok", List.of()));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hola\\nmundo\"}"))
                .andExpect(status().isOk());
    }
}