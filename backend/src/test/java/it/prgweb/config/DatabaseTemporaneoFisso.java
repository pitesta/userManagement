package it.prgweb.config;

import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Base per i test che avviano un contesto Spring Boot completo su un file
 * H2 temporaneo, appena creato (Flyway V1, ddl-auto=validate).
 */
public abstract class DatabaseTemporaneoFisso {

    @TempDir
    static Path dirDatabase;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) throws IOException {
        Files.createDirectories(dirDatabase);
        registry.add("spring.datasource.url", () -> "jdbc:h2:file:"
                + dirDatabase.resolve("app").toAbsolutePath().toString()
                + ";DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE");
    }
}
