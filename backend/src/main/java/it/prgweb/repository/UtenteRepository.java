package it.prgweb.repository;

import it.prgweb.model.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/** Accesso agli account su H2 tramite Spring Data JPA. */
public interface UtenteRepository extends JpaRepository<Utente, Long> {

    Optional<Utente> findByNomeUtente(String nomeUtente);

    long countByNomeUtente(String nomeUtente);

    long countByAmministratoreIsTrue();

    /** Elenco ordinato per nome utente senza senso di maiuscole/minuscole. */
    @Query("select u from Utente u order by lower(u.nomeUtente)")
    List<Utente> elenco();
}
