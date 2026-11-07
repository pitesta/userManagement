package it.prgweb.web;

import it.prgweb.config.DatabaseTemporaneoFisso;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Task 5.2: CORS e SPA fallback come da spec `spa-serving`.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SpaCorsTest extends DatabaseTemporaneoFisso {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Test
    void paginaInizialeDelContesto() throws Exception {
        mockMvc.perform(get("/prgweb/").contextPath("/prgweb"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void deepLinkRicaricatoConIndexHtml() throws Exception {
        mockMvc.perform(get("/prgweb/utenti").contextPath("/prgweb"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void apisEscluseDalFallback() throws Exception {
        // Un percorso API inesistente NON deve convertire in index.html.
        mockMvc.perform(get("/api/qualunque")
                        .header("Authorization", "x"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void assetPresenteServitoConContentType() throws Exception {
        mockMvc.perform(get("/asset-di-prova.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("color: teal")));
    }

    @Test
    void assetMancanteRisponde404SenzaIndex() throws Exception {
        mockMvc.perform(get("/percorso-inesistente.js"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void preflightCorsDalDevServer() throws Exception {
        mockMvc.perform(options("/api/me")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Origin", "http://localhost:4200"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Credentials", "true"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Access-Control-Allow-Methods",
                                org.hamcrest.Matchers.containsString("POST")));
    }

    @Test
    void origineNonConcessaSenzaIntestazioni() throws Exception {
        var risposta = mockMvc.perform(get("/api/me")
                        .header("Origin", "http://localhost:9999"))
                .andExpect(status().isForbidden())
                .andReturn().getResponse();
        assertThat(risposta.getHeader("Access-Control-Allow-Origin")).isNull();
    }
}
