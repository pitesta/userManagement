package it.prgweb.dao;

import it.prgweb.db.Database;
import it.prgweb.model.Utente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Accesso alla tabella UTENTI tramite prepared statement. */
public class UtenteDao {

    private static final String COLONNE =
            "idUtente, nomeUtente, password, email, amministratore";

    public Optional<Utente> findByNomeUtente(String nomeUtente) throws SQLException {
        String sql = "SELECT " + COLONNE + " FROM UTENTI WHERE nomeUtente = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nomeUtente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Utente> findById(long idUtente) throws SQLException {
        String sql = "SELECT " + COLONNE + " FROM UTENTI WHERE idUtente = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idUtente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Utente> findAll() throws SQLException {
        String sql = "SELECT " + COLONNE + " FROM UTENTI ORDER BY nomeUtente COLLATE NOCASE";
        List<Utente> utenti = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                utenti.add(map(rs));
            }
        }
        return utenti;
    }

    /** Inserisce l'utente e valorizza idUtente con la chiave generata. */
    public Utente create(Utente utente) throws SQLException {
        String sql = "INSERT INTO UTENTI (nomeUtente, password, email, amministratore)"
                + " VALUES (?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, utente.getNomeUtente());
            ps.setString(2, utente.getPassword());
            ps.setString(3, utente.getEmail());
            ps.setInt(4, utente.isAmministratore() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    utente.setIdUtente(keys.getLong(1));
                }
            }
        }
        return utente;
    }

    public int count() throws SQLException {
        return count("SELECT COUNT(*) FROM UTENTI");
    }

    public int countAmministratori() throws SQLException {
        return count("SELECT COUNT(*) FROM UTENTI WHERE amministratore = 1");
    }

    public boolean updateAmministratore(long idUtente, boolean amministratore) throws SQLException {
        String sql = "UPDATE UTENTI SET amministratore = ? WHERE idUtente = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, amministratore ? 1 : 0);
            ps.setLong(2, idUtente);
            return ps.executeUpdate() == 1;
        }
    }

    /** Aggiorna anagrafica e hash password. La password nel modello deve essere già un hash. */
    public boolean update(Utente utente) throws SQLException {
        String sql = "UPDATE UTENTI SET nomeUtente = ?, password = ?, email = ?"
                + " WHERE idUtente = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, utente.getNomeUtente());
            ps.setString(2, utente.getPassword());
            ps.setString(3, utente.getEmail());
            ps.setLong(4, utente.getIdUtente());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteById(long idUtente) throws SQLException {
        String sql = "DELETE FROM UTENTI WHERE idUtente = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, idUtente);
            return ps.executeUpdate() == 1;
        }
    }

    private int count(String sql) throws SQLException {
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Utente map(ResultSet rs) throws SQLException {
        Utente u = new Utente();
        u.setIdUtente(rs.getLong("idUtente"));
        u.setNomeUtente(rs.getString("nomeUtente"));
        u.setPassword(rs.getString("password"));
        u.setEmail(rs.getString("email"));
        u.setAmministratore(rs.getInt("amministratore") == 1);
        return u;
    }
}
