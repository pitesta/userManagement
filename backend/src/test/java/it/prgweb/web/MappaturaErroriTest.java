package it.prgweb.web;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.mock.http.MockHttpInputMessage;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

/**
 * Task 4.4: ogni coppia (eccezione, status, messaggio) del mapping
 * GlobalExceptionHandler produce il corpo {"error":"..."} del contratto.
 */
class MappaturaErroriTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new EndpointEspositore())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    /** Endpoint di prova: ogni percorso innesca un'eccezione precisa. */
    @RestController
    static class EndpointEspositore {

        @GetMapping("/eccezione/api")
        public String apiException() {
            throw new ApiException(418, "prova.");
        }

        @GetMapping("/eccezione/corpo")
        public String corpoIlleggibile() {
            throw new HttpMessageNotReadableException("malformato", new MockHttpInputMessage(new byte[0]));
        }

        @GetMapping("/eccezione/unique")
        public String unique() {
            throw new DataIntegrityViolationException("un_utenti_nome_utente", new RuntimeException("dup"));
        }

        @GetMapping("/eccezione/db")
        public TransientDataAccessException db() {
            throw new CannotAcquireLockException("Timeout trying to lock table", new RuntimeException("h2"));
        }

        @GetMapping("/eccezione/metodo")
        public String metodo() throws HttpRequestMethodNotSupportedException {
            throw new HttpRequestMethodNotSupportedException("DELETE");
        }
    }

    @Test
    void coppiaEccezioneStatoEMessaggio() throws Exception {
        mockMvc.perform(get("/eccezione/api"))
                .andExpect(status().is(418))
                .andExpect(content().json("{\"error\":\"prova.\"}"));

        mockMvc.perform(get("/eccezione/corpo"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"Richiesta non valida.\"}"));

        mockMvc.perform(get("/eccezione/unique"))
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"error\":\"Nome utente già registrato.\"}"));

        mockMvc.perform(get("/eccezione/db"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"error\":\"Errore interno del database.\"}"));

        mockMvc.perform(post("/eccezione/api").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
