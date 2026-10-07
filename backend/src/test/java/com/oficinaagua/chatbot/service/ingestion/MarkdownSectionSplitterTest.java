package com.oficinaagua.chatbot.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownSectionSplitterTest {

    private final MarkdownSectionSplitter splitter = new MarkdownSectionSplitter();

    @Test
    void splitsDocumentIntoOneSectionPerLevelTwoHeading() {
        String md = """
                # Tarifas
                > Documento ficticio.

                ## Residencial
                Q 3.50 por m3 extra.

                ## Comercial
                Q 5.00 por m3 extra.
                """;

        List<MarkdownSection> sections = splitter.split(md);

        assertThat(sections).hasSize(2);
        assertThat(sections.get(0)).isEqualTo(
                new MarkdownSection("Tarifas", "Residencial", "Q 3.50 por m3 extra."));
        assertThat(sections.get(1).heading()).isEqualTo("Comercial");
    }

    @Test
    void ignoresTextBeforeFirstSection() {
        String md = "# Titulo\n> aviso\n\n## Seccion\nContenido\n";

        List<MarkdownSection> sections = splitter.split(md);

        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).content()).isEqualTo("Contenido");
    }

    @Test
    void keepsLevelThreeHeadingsInsideTheirSection() {
        String md = "# T\n## A\nuno\n### Sub\ndos\n";

        List<MarkdownSection> sections = splitter.split(md);

        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).content()).contains("### Sub").contains("dos");
    }

    @Test
    void skipsEmptySections() {
        String md = "# T\n## Vacia\n\n## Llena\ntexto\n";

        List<MarkdownSection> sections = splitter.split(md);

        assertThat(sections).extracting(MarkdownSection::heading).containsExactly("Llena");
    }

    @Test
    void handlesWindowsLineEndings() {
        String md = "# T\r\n## A\r\nuno\r\n## B\r\ndos\r\n";

        List<MarkdownSection> sections = splitter.split(md);

        assertThat(sections).extracting(MarkdownSection::content).containsExactly("uno", "dos");
    }

    @Test
    void returnsEmptyListWhenThereAreNoSections() {
        assertThat(splitter.split("# Solo titulo\ntexto suelto")).isEmpty();
    }
}