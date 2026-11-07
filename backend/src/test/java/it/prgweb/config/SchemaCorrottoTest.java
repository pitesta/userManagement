package it.prgweb.config;

import it.prgweb.PrgwebApplication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Task 3.4: su uno schema alterato a mano l'avvio fallisce con l'errore
 * esplicito di validazione dello schema (spec: Validazione dello schema
 * all'avvio), senza proseguire in silenzio.
 */
class SchemaCorrottoTest {

    @TempDir
    Path dirDatabase;

    @Test
    void avvioConSchemaNonAllineatoFallisceEsplicitamente() throws Exception {
        Files.createDirectories(dirDatabase);
        String db = dirDatabase.resolve("app").toAbsolutePath().toString();

        // Schema alterato: manca la colonna email rispetto al modello JPA.
        Properties props = new Properties();
        props.setProperty("user", "sa");
        try (Connection connection = DriverManager.getConnection("jdbc:h2:file:" + db, props);
             Statement statement = connection.createStatement()) {
            statement.execute("create table \"utenti\" ("
                    + " \"id_utente\" bigint not null auto_increment primary key,"
                    + " \"nome_utente\" varchar(100) not null," // "email" assente
                    + " \"password\" varchar(100) not null,"
                    + " \"amministratore\" boolean not null)");
            statement.execute("create table \"app_lock\" (\"id\" bigint not null primary key,"
                    + " \"descrizione\" varchar(255))");
            statement.execute("insert into \"app_lock\" values (1, 'mutex applicativo')");
        }

        String url = "jdbc:h2:file:" + db + ";DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE";

        assertThatExceptionOfType(BeanCreationException.class)
                .isThrownBy(() -> new SpringApplicationBuilder(PrgwebApplication.class)
                        .headless(true)
                        .run(
                                "--spring.datasource.url=" + url,
                                "--spring.flyway.enabled=false",
                                "--spring.main.web-application-type=none"))
                // L'errore di validazione Hibernate indica la colonna mancante.
                .havingCause()
                .withCauseInstanceOf(org.hibernate.tool.schema.spi.SchemaManagementException.class)
                .withMessageContaining("email");
    }
}
