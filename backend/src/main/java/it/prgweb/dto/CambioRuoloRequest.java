package it.prgweb.dto;

/** Corpo della richiesta di cambio ruolo (endpoint amministrativo). */
public record CambioRuoloRequest(long idUtente, boolean amministratore) {
}
