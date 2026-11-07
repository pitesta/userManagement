package it.prgweb.dto;

/** Corpo della richiesta di login. */
public record LoginRequest(String nomeUtente, String password) {
}
