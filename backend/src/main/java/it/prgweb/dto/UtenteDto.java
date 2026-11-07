package it.prgweb.dto;

import it.prgweb.model.Utente;

import java.io.Serializable;

/**
 * Rappresentazione utente per il contratto REST: campi come nel frontend
 * (idUtente, nomeUtente, email, amministratore), mai la password.
 */
public record UtenteDto(long idUtente, String nomeUtente, String email, boolean amministratore)
        implements Serializable {

    public static UtenteDto of(Utente u) {
        return new UtenteDto(u.getIdUtente(), u.getNomeUtente(), u.getEmail(), u.isAmministratore());
    }

    /**
     * Valore per lo store della sessione ("attribute utente"): senza password,
     * serializibile come la risposta utente.
     */
    public UtenteSessione toSessione() {
        return new UtenteSessione(idUtente, nomeUtente, email, amministratore);
    }
}
