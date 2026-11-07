package it.prgweb.config;

import it.prgweb.util.PasswordUtil;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SeedAmministratoreFlywayTest {

    @Test
    void databaseNuovoRiceveUnSoloAdminConPasswordBcrypt() throws Exception {
        DataSource dataSource = nuovoDataSource();
        Flyway.configure().dataSource(dataSource).load().migrate();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT \"nome_utente\", \"password\", \"amministratore\" FROM \"utenti\"")) {
            assertThat(rs.next()).isTrue();
            String hash = rs.getString("password");
            assertThat(rs.getString("nome_utente")).isEqualTo("admin");
            assertThat(rs.getBoolean("amministratore")).isTrue();
            assertThat(hash).isNotEqualTo("admin");
            assertThat(PasswordUtil.check("admin", hash)).isTrue();
            assertThat(rs.next()).isFalse();
        }
    }

    @Test
    void databaseGiaPopolatoNonRiceveUnAltroAccountAdmin() throws Exception {
        DataSource dataSource = nuovoDataSource();
        Flyway.configure().dataSource(dataSource)
                .target(MigrationVersion.fromVersion("1")).load().migrate();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO "utenti"
                    ("nome_utente", "password", "email", "amministratore")
                    VALUES ('esistente', 'hash-test', 'esistente@example.com', TRUE)
                    """);
        }

        Flyway.configure().dataSource(dataSource).load().migrate();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT \"nome_utente\" FROM \"utenti\"")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).isEqualTo("esistente");
            assertThat(rs.next()).isFalse();
        }
    }

    private static DataSource nuovoDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:seed_" + UUID.randomUUID()
                + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }
}
