package it.prgweb.dto;

/** Corpo della richiesta di registrazione. */
public record RegisterRequest(String nomeUtente, String password, String email) {
}
