package it.prgweb.dto;

/** Corpo della richiesta di modifica anagrafica: password opzionale. */
public record ModificaRequest(long idUtente, String nomeUtente, String email, String password) {
}
