package it.prgweb.web;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

/**
 * Trasforma ogni eccezione dell'API in {"error":"<messaggio in italiano>"}
 * con le stesse stringhe di ERRORI_IT nel frontend (design D7). Il formato
 * di errore predefinito del framework non compare mai a questi endpoint.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static ResponseEntity<Map<String, Object>> errore(int status, String messaggio) {
        return ResponseEntity.status(status).body(Map.of("error", messaggio));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException e) {
        return errore(e.getStatus(), e.getMessage());
    }

    /** Corpo assente, JSON malformato o tipi illeggibili → 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> corpoIlleggibile(HttpMessageNotReadableException e) {
        return errore(400, "Richiesta non valida.");
    }

    /**
     * Violazione del vincolo UNIQUE su nome_utente (ad esempio due registrazioni
     * simultanee con lo stesso nome) → 409 con il messaggio del contratto.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> violazioneUnique(DataIntegrityViolationException e) {
        return errore(409, "Nome utente già registrato.");
    }

    /**
     * Guasto tecnico del database, incluse le attese mancate sul mutex
     * (timeout/deadlock → CannotAcquireLockException e QueryTimeoutException).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> guastoDatabase(DataAccessException e) {
        return errore(500, "Errore interno del database.");
    }

    /** Metodo non supportato sul percorso: 405 senza corpo (come l'attuale). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> metodoNonSupportato(
            HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(405).build();
    }

    /** Percorso inesistente (anche risorsa statica'): 404 senza corpo. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> risorsaMancante(NoResourceFoundException e) {
        return ResponseEntity.status(404).build();
    }

    /** Qualsiasi altro errore: nulla del formato predefinito Boot agli endpoint. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> imprevisto(Exception e) {
        return errore(500, "Errore interno del database.");
    }
}
