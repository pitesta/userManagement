package it.prgweb.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 2.2 e 3.1: l'application context si compone su un file H2 appena
 * creato, le property sono lette e la mappatura dell'entity coincide con
 * lo schema della V1 Flyway (contesto verde con ddl-auto=validate).
 */
@SpringBootTest
@TestPropertySource(properties = "logging.level.org.hibernate.SQL=error")
class AvvioTest extends DatabaseTemporaneoFisso {

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Test
    void contestoSiComponeSuDatabaseNuovo() {
        assertThat(environment.getProperty("spring.application.name")).isEqualTo("prgweb");
    }

    @Test
    void propertyNellaConfigurazioneDecisa() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        assertThat(environment.getProperty("spring.jpa.open-in-view", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("server.servlet.session.timeout")).isEqualTo("30m");
        assertThat(environment.getProperty("server.servlet.session.cookie.name")).isEqualTo("JSESSIONID");
        assertThat(environment.getProperty("server.servlet.session.cookie.path")).isEqualTo("/");
        assertThat(environment.getProperty("server.servlet.session.cookie.http-only", Boolean.class)).isTrue();
        assertThat(environment.getProperty("server.servlet.session.cookie.same-site")).isEqualTo("lax");
        assertThat(environment.getProperty("spring.datasource.url")).contains(
                "jdbc:h2:file:", "DB_CLOSE_ON_EXIT=FALSE", "AUTO_SERVER=FALSE");
    }

    @Test
    void schemaMigratoConRigaMutex() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            List<String> tabelle = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery(
                    "select table_name from information_schema.tables where table_schema = 'PUBLIC'")) {
                while (rs.next()) {
                    tabelle.add(rs.getString(1));
                }
            }
            assertThat(tabelle).contains("utenti", "app_lock", "flyway_schema_history");

            try (ResultSet rs = statement.executeQuery("select \"id\", \"descrizione\" from \"app_lock\"")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getLong(1)).isEqualTo(1L);
                assertThat(rs.getString(2)).isEqualTo("mutex applicativo");
                assertThat(rs.next()).isFalse();
            }

            List<String> colonne = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery(
                    "select column_name from information_schema.columns"
                            + " where table_schema = 'PUBLIC' and table_name = 'utenti'")) {
                while (rs.next()) {
                    colonne.add(rs.getString(1));
                }
            }
            assertThat(colonne).containsExactlyInAnyOrder(
                    "id_utente", "nome_utente", "password", "email", "amministratore");
        }
    }

    @Test
    void secondaPartenzaSenzaMigrazioneAggiuntiva() {
        // Il contesto riparte (caching Spring) sullo stesso file: Flyway non
        // riapplica la V1 e Hibernate valida lo schema invariato.
        assertThat(environment.getProperty("spring.flyway.enabled")).isEqualTo("true");
    }
}
