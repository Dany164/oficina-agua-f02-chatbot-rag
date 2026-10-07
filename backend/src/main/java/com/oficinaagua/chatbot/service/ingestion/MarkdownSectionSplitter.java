package com.oficinaagua.chatbot.service.ingestion;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Divide un documento Markdown en secciones usando los encabezados de nivel 2 ("## ").
 */
@Component
public class MarkdownSectionSplitter {

    private static final String TITLE_PREFIX = "# ";
    private static final String SECTION_PREFIX = "## ";

    public List<MarkdownSection> split(String markdown) {
        List<MarkdownSection> sections = new ArrayList<>();
        String title = "";
        String heading = null; // null = todavía no estamos dentro de ninguna sección
        StringBuilder body = new StringBuilder();

        for (String line : markdown.split("\\R")) {
            if (line.startsWith(SECTION_PREFIX)) {
                addIfNotEmpty(sections, title, heading, body);
                heading = line.substring(SECTION_PREFIX.length()).trim();
                body.setLength(0);
            } else if (line.startsWith(TITLE_PREFIX) && title.isEmpty()) {
                title = line.substring(TITLE_PREFIX.length()).trim();
            } else if (heading != null) {
                body.append(line).append('\n');
            }
        }
        addIfNotEmpty(sections, title, heading, body);
        return sections;
    }

    private void addIfNotEmpty(List<MarkdownSection> sections, String title,
                               String heading, StringBuilder body) {
        if (heading == null) {
            return; // texto previo al primer "##": se ignora
        }
        String content = body.toString().strip();
        if (!content.isEmpty()) {
            sections.add(new MarkdownSection(title, heading, content));
        }
    }
}