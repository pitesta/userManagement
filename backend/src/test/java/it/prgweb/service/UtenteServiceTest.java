package it.prgweb.service;

import it.prgweb.config.DatabaseTemporaneoFisso;
import it.prgweb.dto.CambioRuoloRequest;
import it.prgweb.dto.EliminaRequest;
import it.prgweb.dto.ModificaRequest;
import it.prgweb.dto.RegisterRequest;
import it.prgweb.dto.UtenteDto;
import it.prgweb.repository.UtenteRepository;
import it.prgweb.util.PasswordUtil;
import it.prgweb.web.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 4.3: regole di dominio amministrative (elenco, cambio ruolo,
 * modifica, elimina), inclusi i casi "ultimo amministratore".
 */
@SpringBootTest
@Transactional
class UtenteServiceTest extends DatabaseTemporaneoFisso {

    @Autowired
    private AuthService authService;

    @Autowired
    private UtenteService utenteService;

    @Autowired
    private UtenteRepository repository;

    @BeforeEach
    void databasePulitoPerOgniTest() {
        repository.deleteAll();
        repository.flush();
    }

    private UtenteDto registra(String nome) {
        return authService.registra(new RegisterRequest(nome, "segreto1", nome + "@example.com"));
    }

    @Test
    void elencoOrdinatoSenzaSensibilitaMaiuscole() {
        registra("Zeta");
        registra("beta");
        registra("Alpha");

        List<String> nomi = utenteService.elenco().stream().map(UtenteDto::nomeUtente).toList();
        assertThat(nomi).containsExactly("Alpha", "beta", "Zeta");
    }

    @Test
    void cambioRuoloAggiornaIlFlag() {
        registra("admin");
        UtenteDto mario = registra("mario");

        UtenteDto aggiornato = utenteService.cambiaRuolo(new CambioRuoloRequest(mario.idUtente(), true));
        assertThat(aggiornato.amministratore()).isTrue();
        assertThat(repository.countByAmministratoreIsTrue()).isEqualTo(2);

        aggiornato = utenteService.cambiaRuolo(new CambioRuoloRequest(mario.idUtente(), false));
        assertThat(aggiornato.amministratore()).isFalse();
        assertThat(repository.countByAmministratoreIsTrue()).isEqualTo(1);
    }

    @Test
    void degradaUltimoAdminInUltimoAdmin() {
        UtenteDto admin = registra("admin");

        assertThatThrownBy(() -> utenteService.cambiaRuolo(new CambioRuoloRequest(admin.idUtente(), false)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(409);
                    assertThat(e.getMessage()).isEqualTo("Deve rimanere almeno un amministratore.");
                });
        assertThat(repository.findById(admin.idUtente()).orElseThrow().isAmministratore()).isTrue();
    }

    @Test
    void eliminaUltimoAdminRifiutato() {
        UtenteDto admin = registra("admin");

        assertThatThrownBy(() -> utenteService.elimina(new EliminaRequest(admin.idUtente())))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(409);
                    assertThat(e.getMessage()).isEqualTo("Deve rimanere almeno un amministratore.");
                });
        assertThat(repository.countByAmministratoreIsTrue()).isEqualTo(1);
    }

    @Test
    void eliminaQuandoEsisteUnAltroAdmin() {
        registra("admin");
        UtenteDto mario = registra("mario");
        utenteService.cambiaRuolo(new CambioRuoloRequest(mario.idUtente(), true));

        utenteService.elimina(new EliminaRequest(mario.idUtente()));
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.countByAmministratoreIsTrue()).isEqualTo(1);
    }

    @Test
    void modificaAnagraficaConPasswordOpzionale() {
        registra("admin");
        UtenteDto mario = registra("mario");

        // Senza password: credenziali precedenti restano valide.
        UtenteDto aggiornato = utenteService.modifica(
                new ModificaRequest(mario.idUtente(), "mario.bis", "nuovo@example.com", ""));
        assertThat(aggiornato.nomeUtente()).isEqualTo("mario.bis");
        assertThat(aggiornato.email()).isEqualTo("nuovo@example.com");
        assertThat(PasswordUtil.check("segreto1", repository.findById(mario.idUtente())
                .orElseThrow().getPassword())).isTrue();

        // Con password: nuovo hash, memorizzato solo in forma hash.
        utenteService.modifica(new ModificaRequest(mario.idUtente(), "mario.bis",
                "nuovo@example.com", "nuova-Pass-72"));
        String hash = repository.findById(mario.idUtente()).orElseThrow().getPassword();
        assertThat(hash).isNotEqualTo("nuova-Pass-72");
        assertThat(PasswordUtil.check("nuova-Pass-72", hash)).isTrue();
        assertThat(PasswordUtil.check("segreto1", hash)).isFalse();
    }

    @Test
    void omonimoSuRinomina() {
        registra("admin");
        UtenteDto mario = registra("mario");

        assertThatThrownBy(() -> utenteService.modifica(
                new ModificaRequest(mario.idUtente(), "admin", "m@example.com", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(409);
                    assertThat(e.getMessage()).isEqualTo("Nome utente già registrato.");
                });
    }

    @Test
    void operazioniSuUtenteInesistente() {
        registra("admin");

        assertThatThrownBy(() -> utenteService.cambiaRuolo(new CambioRuoloRequest(999, true)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(404);
                    assertThat(e.getMessage()).isEqualTo("Utente non trovato.");
                });
        assertThatThrownBy(() -> utenteService.modifica(
                new ModificaRequest(999, "x", "x@example.com", null)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(404);
                    assertThat(e.getMessage()).isEqualTo("Utente non trovato.");
                });
        assertThatThrownBy(() -> utenteService.elimina(new EliminaRequest(999)))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(404);
                    assertThat(e.getMessage()).isEqualTo("Utente non trovato.");
                });
    }
}
