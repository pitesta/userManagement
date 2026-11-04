package it.prgweb.db;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Connessione SQLite e inizializzazione dello schema. */
public final class Database {

    private static final String DEFAULT_FILE = "data/app.db";

    static {
        // In Tomcat il DriverManager può essere già inizializzato dal classloader
        // comune: la registrazione via ServiceLoader del webapp non avverrebbe.
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("Driver SQLite non trovato: " + e.getMessage());
        }
    }

    private Database() {
    }

    /**
     * Percorso del file SQLite. Ordine di precedenza:
     * -Dapp.db.path, variabile APP_DB_PATH, ${catalina.base}/data/app.db, ./data/app.db.
     */
    public static String getPath() {
        String path = System.getProperty("app.db.path");
        if (path == null || path.isBlank()) {
            path = System.getenv("APP_DB_PATH");
        }
        if (path == null || path.isBlank()) {
            String base = System.getProperty("catalina.base");
            path = (base == null || base.isBlank())
                    ? Path.of(DEFAULT_FILE).toAbsolutePath().toString()
                    : Path.of(base, DEFAULT_FILE).toString();
        }
        return path;
    }

    public static Connection getConnection() throws SQLException {
        Path file = Path.of(getPath()).toAbsolutePath();
        if (file.getParent() != null) {
            try {
                Files.createDirectories(file.getParent());
            } catch (IOException e) {
                throw new SQLException("Impossibile creare la directory del database: " + file.getParent(), e);
            }
        }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    /** Esegue database/schema.sql (idempotente). */
    public static void init() throws SQLException, IOException {
        String script = readSchema();
        try (Connection connection = getConnection(); Statement st = connection.createStatement()) {
            StringBuilder statement = new StringBuilder();
            for (String line : script.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                statement.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    st.execute(statement.toString());
                    statement.setLength(0);
                }
            }
            if (!statement.toString().isBlank()) {
                st.execute(statement.toString());
            }
        }
        try (Connection connection = getConnection(); Statement st = connection.createStatement()) {
            if (!hasColumn(connection, "UTENTI", "amministratore")) {
                st.execute("ALTER TABLE UTENTI ADD COLUMN amministratore INTEGER NOT NULL DEFAULT 0");
                // Bootstrap su DB preesistenti: il primo utente diventa amministratore,
                // altrimenti nessuno potrebbe modificare il flag.
                st.execute("""
                        UPDATE UTENTI SET amministratore = 1
                        WHERE idUtente = (SELECT MIN(idUtente) FROM UTENTI)
                        """);
            }
        }
    }

    private static boolean hasColumn(Connection connection, String table, String column)
            throws SQLException {
        try (Statement st = connection.createStatement();
             var rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String readSchema() throws IOException {
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IOException("schema.sql non trovato nel classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
