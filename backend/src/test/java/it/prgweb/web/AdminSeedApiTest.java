package it.prgweb.web;

import it.prgweb.dto.UtenteSessione;
import it.prgweb.model.Utente;
import it.prgweb.repository.UtenteRepository;
import it.prgweb.util.PasswordUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:adminSeedApi;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE")
@AutoConfigureMockMvc
class AdminSeedApiTest {

    private static final String PASSWORD_MODIFICATA = "AdminNuovaPassword42";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UtenteRepository utenti;

    @Test
    void adminSeedAccedeCambiaLaPropriaPasswordESiAutenticaDiNuovo() throws Exception {
        assertThat(utenti.count()).isEqualTo(1);
        Utente admin = utenti.findByNomeUtente("admin").orElseThrow();
        String hashIniziale = admin.getPassword();
        assertThat(admin.isAmministratore()).isTrue();
        assertThat(PasswordUtil.check("admin", hashIniziale)).isTrue();
        assertThat(hashIniziale).isNotEqualTo("admin");

        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/login").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeUtente\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utente.password").doesNotExist());

        mvc.perform(post("/api/admin/utente/modifica").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUtente\":" + admin.getIdUtente()
                                + ",\"nomeUtente\":\"admin\",\"email\":\"admin.nuovo@example.com\","
                                + "\"password\":\"" + PASSWORD_MODIFICATA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utente.email").value("admin.nuovo@example.com"))
                .andExpect(jsonPath("$.utente.password").doesNotExist());

        Object sessionUser = session.getAttribute(AuthController.SESSION_USER);
        assertThat(sessionUser).isInstanceOf(UtenteSessione.class);
        assertThat(sessionUser.toString()).doesNotContain(hashIniziale, PASSWORD_MODIFICATA);

        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeUtente\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nomeUtente\":\"admin\",\"password\":\""
                                + PASSWORD_MODIFICATA + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utente.amministratore").value(true))
                .andExpect(jsonPath("$.utente.password").doesNotExist());

        String hashNuovo = utenti.findByNomeUtente("admin").orElseThrow().getPassword();
        assertThat(hashNuovo).isNotEqualTo(hashIniziale).isNotEqualTo(PASSWORD_MODIFICATA);
        assertThat(PasswordUtil.check(PASSWORD_MODIFICATA, hashNuovo)).isTrue();
        assertThat(PasswordUtil.check("admin", hashNuovo)).isFalse();
        assertThat(utenti.findById(admin.getIdUtente()).orElseThrow().getEmail())
                .isEqualTo("admin.nuovo@example.com");
        assertThat(utenti.count()).isEqualTo(1);
    }
}
