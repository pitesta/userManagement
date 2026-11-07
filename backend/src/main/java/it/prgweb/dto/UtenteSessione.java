package it.prgweb.dto;

import java.io.Serializable;

/**
 * Valore conservato in sessione HTTP al posto dell'utente completo:
 * senza hash della password (golden F0: l'attributo di sessione "utente").
 */
public record UtenteSessione(long idUtente, String nomeUtente, String email, boolean amministratore)
        implements Serializable {

    public UtenteDto toDto() {
        return new UtenteDto(idUtente, nomeUtente, email, amministratore);
    }
}
