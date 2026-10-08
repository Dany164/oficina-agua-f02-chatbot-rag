package com.oficinaagua.chatbot.service.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.web.client.ResourceAccessException;
import com.oficinaagua.chatbot.exception.AiServiceUnavailableException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    private static final String QUESTION = "¿Cuál es el horario de atención?";

    @Mock
    private KnowledgeRetriever retriever;

    @Mock
    private AnswerGenerator answerGenerator;

    @Captor
    private ArgumentCaptor<RagPrompt> promptCaptor;

    private RagService service;

    @BeforeEach
    void setUp() {
        service = new RagService(retriever, new RagPromptBuilder(6000), answerGenerator);
    }

    private static Document chunk(String text, String documentId, String title, String section) {
        return new Document(text, Map.of("documentId", documentId, "title", title, "section", section));
    }

    private static Document scheduleChunk() {
        return chunk("Horario: lunes a viernes de 8:00 a 16:00.", "01-horarios-y-contacto.md",
                "Horarios y contacto", "Horario de atención");
    }

    @Test
    void answersWithoutCallingTheLlmWhenNothingRelevantWasFound() {
        when(retriever.retrieve(QUESTION)).thenReturn(List.of());

        RagAnswer result = service.answer(QUESTION);

        assertThat(result.answer()).isEqualTo(RagPromptBuilder.NO_INFORMATION_ANSWER);
        assertThat(result.sources()).isEmpty();
        verifyNoInteractions(answerGenerator);
    }

    @Test
    void generatesTheAnswerFromTheRetrievedContext() {
        when(retriever.retrieve(QUESTION)).thenReturn(List.of(scheduleChunk()));
        when(answerGenerator.generate(any(RagPrompt.class)))
                .thenReturn("Atendemos de lunes a viernes de 8:00 a 16:00.");

        RagAnswer result = service.answer(QUESTION);

        verify(answerGenerator).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue().systemMessage()).contains("Horario: lunes a viernes de 8:00 a 16:00.");
        assertThat(promptCaptor.getValue().userMessage()).isEqualTo(QUESTION);
        assertThat(result.answer()).isEqualTo("Atendemos de lunes a viernes de 8:00 a 16:00.");
    }

    @Test
    void returnsTheSourcesOfTheChunksUsed() {
        Document contact = chunk("Teléfono: 5555-0100.", "01-horarios-y-contacto.md",
                "Horarios y contacto", "Teléfonos y correo");
        when(retriever.retrieve(QUESTION)).thenReturn(List.of(scheduleChunk(), contact));
        when(answerGenerator.generate(any(RagPrompt.class))).thenReturn("Respuesta");

        RagAnswer result = service.answer(QUESTION);

        assertThat(result.sources()).containsExactly(
                new RagSource("Horarios y contacto - Horario de atención", "01-horarios-y-contacto.md"),
                new RagSource("Horarios y contacto - Teléfonos y correo", "01-horarios-y-contacto.md"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void answersThereIsNoInformationWhenTheLlmReturnsNothing(String llmText) {
        when(retriever.retrieve(QUESTION)).thenReturn(List.of(scheduleChunk()));
        when(answerGenerator.generate(any(RagPrompt.class))).thenReturn(llmText);

        RagAnswer result = service.answer(QUESTION);

        assertThat(result.answer()).isEqualTo(RagPromptBuilder.NO_INFORMATION_ANSWER);
        assertThat(result.sources()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SIN_INFORMACION", "sin_informacion", "SIN_INFORMACION.", "  SIN_INFORMACION\n"})
    void answersThereIsNoInformationWhenTheLlmReturnsTheMarker(String llmText) {
        when(retriever.retrieve(QUESTION)).thenReturn(List.of(scheduleChunk()));
        when(answerGenerator.generate(any(RagPrompt.class))).thenReturn(llmText);

        RagAnswer result = service.answer(QUESTION);

        assertThat(result.answer()).isEqualTo(RagPromptBuilder.NO_INFORMATION_ANSWER);
        assertThat(result.sources()).isEmpty();
    }

    @Test
    void asksTheLlmToUseTheMarkerWhenTheContextIsNotEnough() {
        when(retriever.retrieve(QUESTION)).thenReturn(List.of(scheduleChunk()));
        when(answerGenerator.generate(any(RagPrompt.class))).thenReturn("Respuesta");

        service.answer(QUESTION);

        verify(answerGenerator).generate(promptCaptor.capture());
        assertThat(promptCaptor.getValue().systemMessage())
                .contains(RagPromptBuilder.NO_INFORMATION_MARKER);
    }

    @Test
    void translatesRetrievalInfrastructureFailuresToServiceUnavailable() {
        when(retriever.retrieve(QUESTION)).thenThrow(new TransientAiException("ollama caido"));

        assertThatThrownBy(() -> service.answer(QUESTION))
                .isInstanceOf(AiServiceUnavailableException.class);
    }

    @Test
    void translatesLlmConnectionFailuresToServiceUnavailable() {
        when(retriever.retrieve(QUESTION)).thenReturn(List.of(scheduleChunk()));
        when(answerGenerator.generate(any(RagPrompt.class)))
                .thenThrow(new ResourceAccessException("connection refused"));

        assertThatThrownBy(() -> service.answer(QUESTION))
                .isInstanceOf(AiServiceUnavailableException.class);
    }

    @Test
    void doesNotHideUnexpectedBugsAsServiceUnavailable() {
        when(retriever.retrieve(QUESTION)).thenThrow(new IllegalStateException("bug"));

        assertThatThrownBy(() -> service.answer(QUESTION))
                .isInstanceOf(IllegalStateException.class);
    }
}