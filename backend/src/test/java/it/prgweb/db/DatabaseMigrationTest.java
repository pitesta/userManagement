package it.prgweb.db;

import it.prgweb.dao.UtenteDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationTest {

    private Path dbFile;

    @BeforeEach
    void setUp() throws Exception {
        Class.forName("org.sqlite.JDBC");
        dbFile = Files.createTempFile("prgweb-migration-", ".db");
        Files.deleteIfExists(dbFile);
        System.setProperty("app.db.path", dbFile.toString());
    }

    @AfterEach
    void tearDown() throws Exception {
        System.clearProperty("app.db.path");
        Files.deleteIfExists(dbFile);
        Files.deleteIfExists(Path.of(dbFile + "-wal"));
        Files.deleteIfExists(Path.of(dbFile + "-shm"));
    }

    @Test
    void aggiungeIlFlagAmministratoreAUnDatabasePreesistente() throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
             Statement st = c.createStatement()) {
            st.execute("""
                    CREATE TABLE UTENTI (
                      idUtente   INTEGER PRIMARY KEY AUTOINCREMENT,
                      nomeUtente VARCHAR(100) NOT NULL UNIQUE,
                      password   VARCHAR(100) NOT NULL,
                      email      VARCHAR(40)  NOT NULL
                    )
                    """);
            st.execute("INSERT INTO UTENTI (nomeUtente, password, email)"
                    + " VALUES ('vecchio', 'hash', 'vecchio@example.com')");
        }

        Database.init();

        try (Connection c = Database.getConnection();
             Statement st = c.createStatement();
             var rs = st.executeQuery("PRAGMA table_info(UTENTI)")) {
            boolean presente = false;
            while (rs.next()) {
                if ("amministratore".equalsIgnoreCase(rs.getString("name"))) {
                    presente = true;
                }
            }
            assertTrue(presente);
        }

        var vecchio = new UtenteDao().findByNomeUtente("vecchio").orElseThrow();
        assertTrue(vecchio.isAmministratore());
    }
}
