package it.prgweb.dto;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 4.1: la serializzazione Jackson 3 produce i campi e i formati del
 * golden F0 e non contiene mai una chiave password.
 */
class SerializzazioneDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void utenteDtoConNomeDeiCampiDelContratto() {
        UtenteDto dto = new UtenteDto(1, "admin", "admin@example.com", true);
        String json = mapper.writeValueAsString(dto);
        assertThat(json).isEqualTo(
                "{\"idUtente\":1,\"nomeUtente\":\"admin\",\"email\":\"admin@example.com\",\"amministratore\":true}");
        assertThat(json).doesNotContain("password");
    }

    @Test
    void utenteSessioneSenzaPassword() {
        UtenteSessione sessione = new UtenteSessione(7, "mario", "m@example.com", false);
        String json = mapper.writeValueAsString(sessione);
        assertThat(json).isEqualTo(
                "{\"idUtente\":7,\"nomeUtente\":\"mario\",\"email\":\"m@example.com\",\"amministratore\":false}");
        assertThat(json).doesNotContain("password");
    }

    @Test
    void recordRichiestaDeserializzati() {
        LoginRequest login = mapper.readValue(
                "{\"nomeUtente\":\"mario\",\"password\":\"xxx\"}", LoginRequest.class);
        assertThat(login.nomeUtente()).isEqualTo("mario");
        assertThat(login.password()).isEqualTo("xxx");

        RegisterRequest register = mapper.readValue(
                "{\"nomeUtente\":\"mario\",\"password\":\"xxx\",\"email\":\"m@example.com\"}",
                RegisterRequest.class);
        assertThat(register.email()).isEqualTo("m@example.com");
    }
}
