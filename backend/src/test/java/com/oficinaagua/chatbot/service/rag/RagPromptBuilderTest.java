package com.oficinaagua.chatbot.service.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

class RagPromptBuilderTest {

    private static Document chunk(String text) {
        return new Document(text, Map.of());
    }

    @Test
    void systemMessageHasTheRulesAndTheRetrievedContext() {
        RagPromptBuilder builder = new RagPromptBuilder(6000);

        RagPrompt prompt = builder.build("¿Cuál es el horario?",
                List.of(chunk("Horario: lunes a viernes de 8:00 a 16:00."), chunk("Teléfono: 5555-0100.")));

        assertThat(prompt.systemMessage())
                .contains("EXCLUSIVAMENTE")
                .contains(RagPromptBuilder.NO_INFORMATION_MARKER)
                .contains("Horario: lunes a viernes de 8:00 a 16:00.")
                .contains("Teléfono: 5555-0100.");
    }

    @Test
    void chunksKeepTheirRelevanceOrder() {
        RagPromptBuilder builder = new RagPromptBuilder(6000);

        RagPrompt prompt = builder.build("pregunta", List.of(chunk("PRIMERO"), chunk("SEGUNDO")));

        assertThat(prompt.systemMessage().indexOf("PRIMERO"))
                .isLessThan(prompt.systemMessage().indexOf("SEGUNDO"));
    }

    @Test
    void userMessageIsTheQuestionUnchanged() {
        RagPromptBuilder builder = new RagPromptBuilder(6000);

        RagPrompt prompt = builder.build("¿Cuánto cuesta {algo} con 100% de descuento?", List.of(chunk("texto")));

        assertThat(prompt.userMessage()).isEqualTo("¿Cuánto cuesta {algo} con 100% de descuento?");
    }

    @Test
    void contextWithSpecialCharactersDoesNotBreakTheTemplate() {
        RagPromptBuilder builder = new RagPromptBuilder(6000);

        RagPrompt prompt = builder.build("pregunta", List.of(chunk("Recargo del 5 % y llaves {x} %s")));

        assertThat(prompt.systemMessage()).contains("Recargo del 5 % y llaves {x} %s");
    }

    @Test
    void dropsWholeChunksThatDoNotFitInTheContextBudget() {
        RagPromptBuilder builder = new RagPromptBuilder(50);
        String first = "A".repeat(30);
        String second = "B".repeat(40);

        RagPrompt prompt = builder.build("pregunta", List.of(chunk(first), chunk(second)));

        assertThat(prompt.systemMessage()).contains(first).doesNotContain("B");
    }

    @Test
    void truncatesTheFirstChunkWhenItAloneExceedsTheBudget() {
        RagPromptBuilder builder = new RagPromptBuilder(10);

        RagPrompt prompt = builder.build("pregunta", List.of(chunk("X".repeat(30))));

        assertThat(prompt.systemMessage()).contains("X".repeat(10)).doesNotContain("X".repeat(11));
    }

    @Test
    void buildsAPromptEvenWithoutChunks() {
        RagPromptBuilder builder = new RagPromptBuilder(6000);

        RagPrompt prompt = builder.build("pregunta", List.of());

        assertThat(prompt.systemMessage()).contains("CONTEXTO:");
    }
}