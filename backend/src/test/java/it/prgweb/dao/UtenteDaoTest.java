package it.prgweb.dao;

import it.prgweb.db.Database;
import it.prgweb.model.Utente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtenteDaoTest {

    private Path dbFile;
    private UtenteDao dao;

    @BeforeEach
    void setUp() throws Exception {
        dbFile = Files.createTempFile("prgweb-test-", ".db");
        Files.deleteIfExists(dbFile);
        System.setProperty("app.db.path", dbFile.toString());
        Database.init();
        dao = new UtenteDao();
    }

    @AfterEach
    void tearDown() throws Exception {
        System.clearProperty("app.db.path");
        Files.deleteIfExists(dbFile);
        Files.deleteIfExists(Path.of(dbFile + "-wal"));
        Files.deleteIfExists(Path.of(dbFile + "-shm"));
    }

    @Test
    void creaELeggeUtente() throws Exception {
        Utente creato = dao.create(utente("mario", "hash1", "mario@example.com"));

        assertTrue(creato.getIdUtente() > 0);
        Optional<Utente> letto = dao.findByNomeUtente("mario");
        assertTrue(letto.isPresent());
        assertEquals("mario@example.com", letto.get().getEmail());
        assertEquals(creato.getIdUtente(), letto.get().getIdUtente());
    }

    @Test
    void elencaUtentiInOrdineAlfabetico() throws Exception {
        dao.create(utente("zelda", "hash", "z@example.com"));
        dao.create(utente("anna", "hash", "a@example.com"));

        List<Utente> utenti = dao.findAll();
        assertEquals(List.of("anna", "zelda"), utenti.stream().map(Utente::getNomeUtente).toList());
    }

    @Test
    void nomeUtenteDuplicatoVieneRifiutato() throws Exception {
        dao.create(utente("mario", "hash", "mario@example.com"));
        assertThrows(Exception.class,
                () -> dao.create(utente("mario", "altro", "altro@example.com")));
    }

    @Test
    void flagAmministratoreVieneSalvatoEAggiornato() throws Exception {
        Utente mario = utente("mario", "hash", "mario@example.com");
        mario.setAmministratore(true);
        dao.create(mario);
        dao.create(utente("luigi", "hash", "luigi@example.com"));

        assertTrue(dao.findByNomeUtente("mario").orElseThrow().isAmministratore());
        assertFalse(dao.findByNomeUtente("luigi").orElseThrow().isAmministratore());
        assertEquals(1, dao.countAmministratori());
        assertEquals(2, dao.count());

        assertTrue(dao.updateAmministratore(
                dao.findByNomeUtente("luigi").orElseThrow().getIdUtente(), true));
        assertEquals(2, dao.countAmministratori());
        assertTrue(dao.findByNomeUtente("luigi").orElseThrow().isAmministratore());
    }

    @Test
    void aggiornaAnagraficaEPassword() throws Exception {
        Utente mario = dao.create(utente("mario", "hash1", "mario@example.com"));

        mario.setNomeUtente("mario.rossi");
        mario.setEmail("mario.rossi@example.com");
        mario.setPassword("hash2");

        assertTrue(dao.update(mario));

        Utente letto = dao.findByNomeUtente("mario.rossi").orElseThrow();
        assertEquals("mario.rossi@example.com", letto.getEmail());
        assertEquals("hash2", letto.getPassword());
        assertTrue(dao.findByNomeUtente("mario").isEmpty());
    }

    @Test
    void aggiornamentoConNomeDuplicatoVieneRifiutato() throws Exception {
        Utente mario = dao.create(utente("mario", "hash", "mario@example.com"));
        dao.create(utente("luigi", "hash", "luigi@example.com"));

        mario.setNomeUtente("luigi");

        assertThrows(Exception.class, () -> dao.update(mario));
        assertEquals("mario", dao.findById(mario.getIdUtente()).orElseThrow().getNomeUtente());
    }

    @Test
    void eliminaUtentePerId() throws Exception {
        Utente mario = dao.create(utente("mario", "hash", "mario@example.com"));

        assertTrue(dao.deleteById(mario.getIdUtente()));
        assertTrue(dao.findByNomeUtente("mario").isEmpty());
        assertEquals(0, dao.count());
        assertFalse(dao.deleteById(mario.getIdUtente()));
    }

    private Utente utente(String nome, String password, String email) {
        Utente u = new Utente();
        u.setNomeUtente(nome);
        u.setPassword(password);
        u.setEmail(email);
        return u;
    }
}
