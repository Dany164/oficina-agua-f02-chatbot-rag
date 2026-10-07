package com.oficinaagua.chatbot.service.ingestion;


/**
 * Una sección de un documento Markdown: lo que se convertirá en un chunk.
 *
 * @param documentTitle título del documento (línea "# ...")
 * @param heading       encabezado de la sección (línea "## ...")
 * @param content       texto de la sección, sin el encabezado
 */

public record MarkdownSection(String documentTitle, String heading, String content) {
}
