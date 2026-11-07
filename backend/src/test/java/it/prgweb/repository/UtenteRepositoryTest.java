package it.prgweb.repository;

import it.prgweb.model.Utente;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 3.2: CRUD, identity crescente, unique sul nome utente (violation →
 * conflitto), mapping boolean e ordine senza sensibilità di mailuscole.
 */
@DataJpaTest
class UtenteRepositoryTest {

    @Autowired
    private UtenteRepository repository;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void databasePulitoPerOgniTest() {
        repository.deleteAll();
        repository.flush();
    }

    private Utente utente(String nome, String email, boolean admin) {
        Utente u = new Utente();
        u.setNomeUtente(nome);
        u.setPassword("$2a$10$abcdefghijklmnopqrstuv0123456789012345678901234567890");
        u.setEmail(email);
        u.setAmministratore(admin);
        return u;
    }

    @Test
    void crudCompleto() {
        Utente salvato = repository.save(utente("mario", "mario@example.com", false));
        em.flush();
        em.clear();

        assertThat(salvato.getIdUtente()).isPositive();
        assertThat(repository.findById(salvato.getIdUtente()))
                .hasValueSatisfying(u -> {
                    assertThat(u.getNomeUtente()).isEqualTo("mario");
                    assertThat(u.getEmail()).isEqualTo("mario@example.com");
                    assertThat(u.isAmministratore()).isFalse();
                })
                .hasValueSatisfying(u -> assertThat(u.getPassword()).startsWith("$2a$"));

        repository.deleteById(salvato.getIdUtente());
        assertThat(repository.findById(salvato.getIdUtente())).isEmpty();
    }

    @Test
    void identitaCrescente_e_nonRiutilizzatiDopoDelete() {
        Utente primo = repository.save(utente("uno", "uno@example.com", false));
        Utente secondo = repository.save(utente("due", "due@example.com", false));
        Utente terzo = repository.save(utente("tre", "tre@example.com", true));
        em.flush();

        assertThat(primo.getIdUtente()).isLessThan(secondo.getIdUtente());
        assertThat(secondo.getIdUtente()).isLessThan(terzo.getIdUtente());

        repository.delete(terzo);
        em.flush();
        Utente quarto = repository.save(utente("quattro", "quattro@example.com", false));
        em.flush();
        assertThat(quarto.getIdUtente()).isGreaterThan(secondo.getIdUtente());
    }

    @Test
    void uniqueNomeUtente_conflitto() {
        repository.saveAndFlush(utente("mario", "mario@example.com", false));
        assertThatThrownBy(() -> repository.saveAndFlush(utente("mario", "altro@example.com", false)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("un_utenti_nome_utente");
    }

    @Test
    void mappingBooleanAmministratore() {
        repository.save(utente("admin", "admin@example.com", true));
        repository.save(utente("mario", "mario@example.com", false));
        em.flush();
        em.clear();

        assertThat(repository.findAll())
                .anySatisfy(u -> assertThat(u.getNomeUtente()).isEqualTo("admin"))
                .anySatisfy(u -> assertThat(u.isAmministratore()).isFalse());
        assertThat(repository.countByAmministratoreIsTrue()).isEqualTo(1);
        assertThat(repository.findByNomeUtente("admin")).isPresent();
        assertThat(repository.findByNomeUtente("marioXX")).isEmpty();
    }

    @Test
    void elencoOrdinatoSenzaSensibilitaMaiuscole() {
        repository.save(utente("Zeta", "z@example.com", false));
        repository.save(utente("beta", "b@example.com", false));
        repository.save(utente("Alpha", "a@example.com", false));
        em.flush();
        em.clear();

        List<String> nomi = repository.elenco().stream().map(Utente::getNomeUtente).toList();
        assertThat(nomi).containsExactly("Alpha", "beta", "Zeta");
    }
}
