package com.oficinaagua.chatbot.service.ingestion;

import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Lanza la ingesta de la carpeta de conocimiento al arrancar con el perfil "ingest".
 * En ejecución normal (sin ese perfil) este componente ni se crea.
 */
@Component
@Profile("ingest")
public class KnowledgeIngestionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionRunner.class);

    private final KnowledgeIngestionService ingestionService;
    private final String knowledgePath;

    public KnowledgeIngestionRunner(KnowledgeIngestionService ingestionService,
                                    @Value("${chatbot.ingestion.knowledge-path}") String knowledgePath) {
        this.ingestionService = ingestionService;
        this.knowledgePath = knowledgePath;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path directory = Path.of(knowledgePath).toAbsolutePath().normalize();
        log.info("Iniciando ingesta desde {}", directory);

        Map<String, IngestionOutcome> results = ingestionService.ingestDirectory(directory);

        long ingested = results.values().stream()
                .filter(outcome -> outcome == IngestionOutcome.INGESTED).count();
        log.info("Ingesta terminada: {} documentos, {} ingeridos, {} sin cambios",
                results.size(), ingested, results.size() - ingested);
    }
}