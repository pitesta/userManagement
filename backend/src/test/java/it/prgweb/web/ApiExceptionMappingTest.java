package it.prgweb.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica le coppie status/messaggio per ogni errore emesso dal backend F0. */
class ApiExceptionMappingTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void tutteLeEccezioniDiDominioMantengonoStatusECorpoGolden() {
        List<Caso> casi = List.of(
                new Caso(ApiException.nonAutenticato(), 401, "Non autenticato."),
                new Caso(ApiException.riservatoAmministratori(), 403, "Operazione riservata agli amministratori."),
                new Caso(ApiException.utenteNonValido(), 400, "Utente non valido."),
                new Caso(ApiException.utenteNonTrovato(), 404, "Utente non trovato."),
                new Caso(ApiException.ultimoAmministratore(), 409, "Deve rimanere almeno un amministratore."),
                new Caso(ApiException.richiestaNonValida(), 400, "Richiesta non valida."),
                new Caso(ApiException.credenzialiMancanti(), 400, "Inserire nome utente e password."),
                new Caso(ApiException.credenzialiNonValide(), 401, "Credenziali non valide."),
                new Caso(ApiException.nomeObbligatorio(), 400, "Il nome utente è obbligatorio."),
                new Caso(ApiException.nomeTroppoLungo(), 400, "Il nome utente non può superare 100 caratteri."),
                new Caso(ApiException.passwordCorta(), 400, "La password deve contenere almeno 6 caratteri."),
                new Caso(ApiException.passwordTroppoLunga(), 400, "La password è troppo lunga (massimo 72 byte)."),
                new Caso(ApiException.emailObbligatoria(), 400, "L'email è obbligatoria."),
                new Caso(ApiException.emailTroppoLunga(), 400, "L'email non può superare 40 caratteri."),
                new Caso(ApiException.emailNonValida(), 400, "L'email non è valida."),
                new Caso(ApiException.nomeGiaRegistrato(), 409, "Nome utente già registrato."),
                new Caso(ApiException.erroreDatabase(), 500, "Errore interno del database."));

        for (Caso caso : casi) {
            ResponseEntity<Map<String, Object>> response = handler.api(caso.eccezione());
            assertThat(response.getStatusCode().value()).as(caso.messaggio()).isEqualTo(caso.status());
            assertThat(response.getBody()).as(caso.messaggio()).containsExactly(Map.entry("error", caso.messaggio()));
        }
    }

    private record Caso(ApiException eccezione, int status, String messaggio) {}
}
