package com.oficinaagua.chatbot.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MonolithCatalogRepositoryTest {

    @Test
    void buildsAUrlThatVerifiesTheServerCertificateWithTheGivenCa() {
        String url = MonolithCatalogRepository.jdbcUrl(
                "db.example.org", 3306, "oficina_agua", Path.of("certs/ca.pem"));

        assertThat(url)
                .startsWith("jdbc:mariadb://db.example.org:3306/oficina_agua?")
                .contains("sslMode=verify-full")
                .contains("serverSslCert=")
                .endsWith("/certs/ca.pem");
    }

    @Test
    void usesForwardSlashesInTheCertificatePathEvenOnWindows() {
        String url = MonolithCatalogRepository.jdbcUrl(
                "db.example.org", 3306, "oficina_agua", Path.of("certs", "ca.pem"));

        assertThat(url).doesNotContain("\\");
    }
}