package it.prgweb.web;

import it.prgweb.config.DatabaseTemporaneoFisso;
import it.prgweb.dto.RegisterRequest;
import it.prgweb.repository.UtenteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Contract HTTP dei controller contro i comportamenti registrati nei golden F0. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiContractTest extends DatabaseTemporaneoFisso {

    @Autowired MockMvc mvc;
    @Autowired UtenteRepository utenti;
    @Autowired Environment environment;

    @BeforeEach
    void databaseVuoto() {
        utenti.deleteAll();
    }

    @Test
    void registrazioneValidaDuplicataEValidazione() throws Exception {
        mvc.perform(post("/api/register").contentType(APPLICATION_JSON)
                        .content("{\"nomeUtente\":\" admin \",\"password\":\"segreto1\",\"email\":\" admin@example.com \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.utente.nomeUtente").value("admin"))
                .andExpect(jsonPath("$.utente.amministratore").value(true))
                .andExpect(jsonPath("$.utente.password").doesNotExist());

        mvc.perform(post("/api/register").contentType(APPLICATION_JSON)
                        .content("{\"nomeUtente\":\"mario\",\"password\":\"segreto1\",\"email\":\"mario@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.utente.amministratore").value(false));

        mvc.perform(post("/api/register").contentType(APPLICATION_JSON)
                        .content("{\"nomeUtente\":\"admin\",\"password\":\"segreto1\",\"email\":\"admin@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"error\":\"Nome utente già registrato.\"}"));

        mvc.perform(post("/api/register").contentType(APPLICATION_JSON)
                        .content("{\"nomeUtente\":\"\",\"password\":\"segreto1\",\"email\":\"a@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"Il nome utente è obbligatorio.\"}"));

        mvc.perform(post("/api/register").contentType(APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"Richiesta non valida.\"}"));
    }

    @Test
    void tutteLeValidazioniHttpDiRegistrazioneRispettanoIlGolden() throws Exception {
        String nomeTroppoLungo = "n".repeat(101);
        String passwordTroppoLunga = "p".repeat(73);
        String emailTroppoLunga = "a".repeat(32) + "@example.test";

        aspettatiErroreRegistrazione("", "Il nome utente è obbligatorio.");
        aspettatiErroreRegistrazione(nomeTroppoLungo, "Il nome utente non può superare 100 caratteri.");
        aspettatiErroreRegistrazione("mario", "La password deve contenere almeno 6 caratteri.", "12345");
        aspettatiErroreRegistrazione("mario", "La password è troppo lunga (massimo 72 byte).", passwordTroppoLunga);
        aspettatiErroreRegistrazione("mario", "L'email è obbligatoria.", "segreto1", "");
        aspettatiErroreRegistrazione("mario", "L'email non può superare 40 caratteri.", "segreto1", emailTroppoLunga);
        aspettatiErroreRegistrazione("mario", "L'email non è valida.", "segreto1", "non-valida");
    }

    @Test
    void idAmministrativoNonValidoRestaNelContratto() throws Exception {
        registra("admin", "admin@example.com");
        MockHttpSession admin = login("admin");

        mvc.perform(post("/api/admin/utente").session(admin).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":0,\"amministratore\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"Utente non valido.\"}"));

        mvc.perform(post("/api/admin/utente/modifica").session(admin).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":0,\"nomeUtente\":\"x\",\"email\":\"x@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"Utente non valido.\"}"));
    }

    private void aspettatiErroreRegistrazione(String nomeUtente, String messaggio) throws Exception {
        aspettatiErroreRegistrazione(nomeUtente, messaggio, "segreto1", "mario@example.com");
    }

    private void aspettatiErroreRegistrazione(String nomeUtente, String messaggio,
                                               String password) throws Exception {
        aspettatiErroreRegistrazione(nomeUtente, messaggio, password, "mario@example.com");
    }

    private void aspettatiErroreRegistrazione(String nomeUtente, String messaggio,
                                               String password, String email) throws Exception {
        String body = "{\"nomeUtente\":\"" + nomeUtente + "\",\"password\":\"" + password
                + "\",\"email\":\"" + email + "\"}";
        mvc.perform(post("/api/register").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"" + messaggio + "\"}"));
    }

    @Test
    void loginMeListaLogoutECookieGolden() throws Exception {
        registra("admin", "admin@example.com");
        registra("mario", "mario@example.com");

        MockHttpSession sessione = new MockHttpSession();
        String idPrima = sessione.getId();
        MvcResult login = mvc.perform(post("/api/login").session(sessione).contentType(APPLICATION_JSON)
                        .content("{\"nomeUtente\":\" mario \",\"password\":\"segreto1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utente.nomeUtente").value("mario"))
                .andExpect(jsonPath("$.utente.password").doesNotExist())
                .andReturn();

        assertThat(environment.getProperty("server.servlet.session.cookie.name")).isEqualTo("JSESSIONID");
        assertThat(environment.getProperty("server.servlet.session.cookie.path")).isEqualTo("/");
        assertThat(environment.getProperty("server.servlet.session.cookie.http-only", Boolean.class)).isTrue();
        assertThat(environment.getProperty("server.servlet.session.cookie.same-site")).isEqualTo("lax");
        assertThat(login.getRequest().getSession(false).getId()).isNotEqualTo(idPrima);

        mvc.perform(get("/api/me").session(sessione))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utente.nomeUtente").value("mario"));
        mvc.perform(get("/api/utenti").session(sessione))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utenti.length()").value(2))
                .andExpect(jsonPath("$.utenti[0].password").doesNotExist());

        mvc.perform(post("/api/logout").session(sessione))
                .andExpect(status().isOk()).andExpect(content().json("{\"ok\":true}"));
        mvc.perform(get("/api/me").session(sessione))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Non autenticato.\"}"));
    }

    @Test
    void operazioniAmministrativeERuoloDiSessione() throws Exception {
        long idAdmin = registra("admin", "admin@example.com");
        long idMario = registra("mario", "mario@example.com");
        MockHttpSession admin = login("admin");

        mvc.perform(post("/api/admin/utente").session(admin).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":" + idMario + ",\"amministratore\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.utente.amministratore").value(true));

        mvc.perform(post("/api/admin/utente/modifica").session(admin).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":" + idMario + ",\"nomeUtente\":\"mario2\",\"email\":\"mario2@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.utente.nomeUtente").value("mario2"));

        mvc.perform(post("/api/admin/utente/elimina").session(admin).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":" + idMario + "}"))
                .andExpect(status().isOk()).andExpect(content().json("{\"ok\":true,\"logout\":false}"));

        mvc.perform(post("/api/admin/utente").session(admin).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":" + idAdmin + ",\"amministratore\":false}"))
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"error\":\"Deve rimanere almeno un amministratore.\"}"));

        mvc.perform(post("/api/admin/utente/elimina").session(admin).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":99999}"))
                .andExpect(status().isNotFound())
                .andExpect(content().json("{\"error\":\"Utente non trovato.\"}"));
    }

    @Test
    void sessioneNonAutenticataENonAdminRiceveGliErroriGolden() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Non autenticato.\"}"));
        mvc.perform(post("/api/login").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"error\":\"Inserire nome utente e password.\"}"));
        mvc.perform(post("/api/login").contentType(APPLICATION_JSON)
                        .content("{\"nomeUtente\":\"nessuno\",\"password\":\"segreto1\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Credenziali non valide.\"}"));

        registra("admin", "admin@example.com");
        long idMario = registra("mario", "mario@example.com");
        MockHttpSession mario = login("mario");
        mvc.perform(post("/api/admin/utente").session(mario).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":" + idMario + ",\"amministratore\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"error\":\"Operazione riservata agli amministratori.\"}"));
    }

    @Test
    void selfUpdateAggiornaSessioneESelfDeleteInvalidaSessione() throws Exception {
        long idAdmin = registra("admin", "admin@example.com");
        long idHelper = registra("helper", "helper@example.com");
        long idKeeper = registra("keeper", "keeper@example.com");
        MockHttpSession admin = login("admin");
        mvc.perform(post("/api/admin/utente").session(admin).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":" + idHelper + ",\"amministratore\":true}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/admin/utente").session(admin).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":" + idKeeper + ",\"amministratore\":true}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/admin/utente/modifica").session(admin).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":" + idAdmin + ",\"nomeUtente\":\"admin2\",\"email\":\"admin2@example.com\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/me").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utente.nomeUtente").value("admin2"));

        mvc.perform(post("/api/admin/utente").session(admin).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":" + idAdmin + ",\"amministratore\":false}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/admin/utente").session(admin).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":" + idHelper + ",\"amministratore\":false}"))
                .andExpect(status().isForbidden());

        MockHttpSession helper = login("helper");
        mvc.perform(post("/api/admin/utente/elimina").session(helper).contentType(APPLICATION_JSON)
                        .content("{\"idUtente\":" + idHelper + "}"))
                .andExpect(status().isOk()).andExpect(content().json("{\"ok\":true,\"logout\":true}"));
        mvc.perform(get("/api/me").session(helper))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Non autenticato.\"}"));
    }

    private long registra(String nome, String email) throws Exception {
        MvcResult risultato = mvc.perform(post("/api/register").contentType(APPLICATION_JSON)
                        .content("{\"nomeUtente\":\"" + nome + "\",\"password\":\"segreto1\",\"email\":\"" + email + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        String body = risultato.getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll(".*\\\"idUtente\\\":([0-9]+).*", "$1"));
    }

    private MockHttpSession login(String nome) throws Exception {
        MvcResult risultato = mvc.perform(post("/api/login").contentType(APPLICATION_JSON)
                        .content("{\"nomeUtente\":\"" + nome + "\",\"password\":\"segreto1\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) risultato.getRequest().getSession(false);
    }
}
