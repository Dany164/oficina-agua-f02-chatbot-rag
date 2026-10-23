package com.oficinaagua.chatbot.service.monolith;

import com.oficinaagua.chatbot.repository.MonolithCatalogRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Regenera knowledge/03-tarifas.md con los datos vigentes del monolito al arrancar con el perfil
 * "import-monolith". Corre antes que la ingesta (@Order(1)) para que, al combinar ambos perfiles,
 * el documento nuevo se indexe en la misma ejecución.
 */
@Component
@Profile("import-monolith")
@Order(1)
public class MonolithImportRunner implements ApplicationRunner {

    static final String TARIFF_FILE_NAME = "03-tarifas.md";

    private static final Logger log = LoggerFactory.getLogger(MonolithImportRunner.class);

    private final MonolithCatalogRepository repository;
    private final TariffDocumentGenerator generator;
    private final String knowledgePath;

    public MonolithImportRunner(MonolithCatalogRepository repository,
                                TariffDocumentGenerator generator,
                                @Value("${chatbot.ingestion.knowledge-path}") String knowledgePath) {
        this.repository = repository;
        this.generator = generator;
        this.knowledgePath = knowledgePath;
    }

    @Override
    public void run(ApplicationArguments args) {
        PublicCatalog catalog = repository.load();
        log.info("Catálogo leído del monolito: {} tipos de servicio, {} tarifas, {} métodos de pago",
                catalog.serviceTypes().size(), catalog.tariffs().size(), catalog.paymentMethods().size());

        String markdown = generator.generate(catalog, LocalDate.now());
        Path target = Path.of(knowledgePath).toAbsolutePath().normalize().resolve(TARIFF_FILE_NAME);
        try {
            Files.writeString(target, markdown, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir " + target, e);
        }
        log.info("Documento de tarifas escrito en {}", target);
    }
}