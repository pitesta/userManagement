package it.prgweb.web;

import it.prgweb.dto.CambioRuoloRequest;
import it.prgweb.dto.EliminaRequest;
import it.prgweb.dto.ModificaRequest;
import it.prgweb.dto.UtenteDto;
import it.prgweb.dto.UtenteSessione;
import it.prgweb.service.UtenteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Operazioni riservate agli amministratori: cambio ruolo, modifica
 * dell'anagrafica ed eliminazione di un utente.
 */
@RestController
public class AdminUtenteController {

    private final UtenteService utenteService;

    public AdminUtenteController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    @PostMapping("/api/admin/utente")
    public ResponseEntity<Map<String, Object>> cambiaRuolo(HttpServletRequest req,
                                                           @RequestBody CambioRuoloRequest richiesta) {
        UtenteSessione corrente = richiediAdmin(req);

        if (richiesta.idUtente() <= 0) {
            throw ApiException.utenteNonValido();
        }

        UtenteDto aggiornato = utenteService.cambiaRuolo(richiesta);
        if (aggiornato.idUtente() == corrente.idUtente()) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.setAttribute(AuthController.SESSION_USER, aggiornato.toSessione());
            }
        }

        return ResponseEntity.ok(Map.of("utente", aggiornato));
    }

    @PostMapping("/api/admin/utente/modifica")
    public ResponseEntity<Map<String, Object>> modifica(HttpServletRequest req,
                                                        @RequestBody ModificaRequest richiesta) {
        UtenteSessione corrente = richiediAdmin(req);

        if (richiesta.idUtente() <= 0) {
            throw ApiException.utenteNonValido();
        }

        String nomeUtente = trim(richiesta.nomeUtente());
        String email = trim(richiesta.email());
        String password = richiesta.password();
        boolean cambiaPassword = password != null && !password.isBlank();

        String errore = AuthController.validaAnagrafica(nomeUtente, email,
                cambiaPassword ? password : null);
        if (errore != null) {
            throw new ApiException(400, errore);
        }

        UtenteDto aggiornato = utenteService.modifica(
                new ModificaRequest(richiesta.idUtente(), nomeUtente, email, richiesta.password()));
        if (aggiornato.idUtente() == corrente.idUtente()) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.setAttribute(AuthController.SESSION_USER, aggiornato.toSessione());
            }
        }

        return ResponseEntity.ok(Map.of("utente", aggiornato));
    }

    @PostMapping("/api/admin/utente/elimina")
    public ResponseEntity<Map<String, Object>> elimina(HttpServletRequest req,
                                                       @RequestBody EliminaRequest richiesta) {
        UtenteSessione corrente = richiediAdmin(req);

        if (richiesta.idUtente() <= 0) {
            throw ApiException.utenteNonValido();
        }

        utenteService.elimina(richiesta);

        boolean logout = richiesta.idUtente() == corrente.idUtente();
        if (logout) {
            HttpSession session = req.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }

        return ResponseEntity.ok(Map.of("ok", true, "logout", logout));
    }

    private static UtenteSessione richiediAdmin(HttpServletRequest req) {
        UtenteSessione corrente = AuthController.sessione(req);
        if (corrente == null) {
            throw ApiException.nonAutenticato();
        }
        if (!corrente.amministratore()) {
            throw ApiException.riservatoAmministratori();
        }
        return corrente;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
