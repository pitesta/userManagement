package it.prgweb.web;

import it.prgweb.dto.UtenteDto;
import it.prgweb.dto.UtenteSessione;
import it.prgweb.service.UtenteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Elenco utenti registrati; richiede una sessione autenticata. */
@RestController
public class UtentiController {

    private final UtenteService utenteService;

    public UtentiController(UtenteService utenteService) {
        this.utenteService = utenteService;
    }

    @GetMapping("/api/utenti")
    public ResponseEntity<Map<String, Object>> utenti(HttpServletRequest req) {
        // Golden F0: qualunque utente autenticato (anche non amministratore).
        UtenteSessione corrente = AuthController.sessione(req);
        if (corrente == null) {
            throw ApiException.nonAutenticato();
        }

        List<UtenteDto> utenti = utenteService.elenco();
        return ResponseEntity.ok(Map.of("utenti", utenti));
    }
}
