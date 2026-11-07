package it.prgweb.service;

import it.prgweb.config.DatabaseTemporaneoFisso;
import it.prgweb.dto.RegisterRequest;
import it.prgweb.dto.UtenteDto;
import it.prgweb.web.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task 4.2: registra (primo/successivo, duplicato → conflitto) e login
 * (check fuori transazione) secondo il golden F0.
 */
@SpringBootTest
@Transactional
class AuthServiceTest extends DatabaseTemporaneoFisso {

    @Autowired
    private AuthService authService;

    private UtenteDto registra(String nome) {
        return authService.registra(new RegisterRequest(nome, "segreto1", nome + "@example.com"));
    }

    @Test
    void amministratoreSeeded_eNuovaRegistrazioneNonAmministratore() {
        UtenteDto primo = authService.login(new it.prgweb.dto.LoginRequest("admin", "admin"));
        assertThat(primo.amministratore()).isTrue();
        assertThat(primo.nomeUtente()).isEqualTo("admin");

        UtenteDto registrato = registra("mario");
        assertThat(registrato.amministratore()).isFalse();
    }

    @Test
    void nomeDuplicatoGeneraConflittoNelMessaggioDelContratto() {
        registra("mario");
        assertThatThrownBy(() -> registra("mario"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(409);
                    assertThat(e.getMessage()).isEqualTo("Nome utente già registrato.");
                });
    }

    @Test
    void loginConCredenzialiVerificateAlDiFuoriDellaTransazione() {
        registra("mario");

        UtenteDto esito = authService.login(new it.prgweb.dto.LoginRequest("mario", "segreto1"));
        assertThat(esito.nomeUtente()).isEqualTo("mario");

        assertThatThrownBy(() -> authService.login(new it.prgweb.dto.LoginRequest("mario", "sbadagliata")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(401));

        assertThatThrownBy(() -> authService.login(new it.prgweb.dto.LoginRequest("inesistente", "segreto1")))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(401));
    }

    @Test
    void passwordNonEspostaNellaRisposta() {
        UtenteDto dto = registra("wilma");
        String json = new tools.jackson.databind.ObjectMapper().writeValueAsString(dto);
        assertThat(json).doesNotContain("password", "hash");
    }
}
