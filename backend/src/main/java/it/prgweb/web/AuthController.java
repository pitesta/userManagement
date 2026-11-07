package it.prgweb.web;

import it.prgweb.dto.LoginRequest;
import it.prgweb.dto.RegisterRequest;
import it.prgweb.dto.UtenteDto;
import it.prgweb.dto.UtenteSessione;
import it.prgweb.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Login, registrazione, logout e utente corrente. Percorsi e verbi come nel
 * contratto ATTUALE: POST /api/login|register|logout, GET /api/me.
 */
@RestController
public class AuthController {

    public static final String SESSION_USER = "utente";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_NOME = 100;
    private static final int MAX_EMAIL = 40;
    private static final int MAX_PASSWORD_BYTES = 72; // limite BCrypt

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/login")
    public ResponseEntity<Map<String, Object>> login(HttpServletRequest req,
                                                     @RequestBody LoginRequest richiesta) {
        String nomeUtente = trim(richiesta == null ? null : richiesta.nomeUtente());
        String password = richiesta == null ? null : richiesta.password();

        if (nomeUtente == null || nomeUtente.isEmpty() || password == null || password.isEmpty()) {
            throw ApiException.credenzialiMancanti();
        }

        UtenteDto utente = authService.login(new LoginRequest(nomeUtente, password));

        // Sessione creata dopo il controllo delle credenziali: un solo header
        // Set-Cookie con l'identificatore finale dopo changeSessionId (golden F0).
        HttpSession session = req.getSession(true);
        req.changeSessionId();
        session.setAttribute(SESSION_USER, utente.toSessione());

        return ResponseEntity.ok(Map.of("utente", utente));
    }

    @PostMapping("/api/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest richiesta) {
        String nomeUtente = trim(richiesta == null ? null : richiesta.nomeUtente());
        String password = richiesta == null ? null : richiesta.password();
        String email = trim(richiesta == null ? null : richiesta.email());

        String errore = validaRegistrazione(nomeUtente, password, email);
        if (errore != null) {
            throw new ApiException(400, errore);
        }

        UtenteDto utente = authService.registra(
                new RegisterRequest(nomeUtente, password, email));

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("utente", utente));
    }

    @PostMapping("/api/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/api/me")
    public ResponseEntity<Map<String, Object>> me(HttpServletRequest req) {
        UtenteSessione corrente = sessione(req);
        if (corrente == null) {
            throw ApiException.nonAutenticato();
        }
        return ResponseEntity.ok(Map.of("utente", corrente.toDto()));
    }

    static UtenteSessione sessione(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER);
        return value instanceof UtenteSessione utente ? utente : null;
    }

    // ---- ordine di validazione del golden F0 ----

    static String validaRegistrazione(String nomeUtente, String password, String email) {
        if (nomeUtente == null || nomeUtente.isEmpty()) {
            return "Il nome utente è obbligatorio.";
        }
        if (nomeUtente.length() > MAX_NOME) {
            return "Il nome utente non può superare 100 caratteri.";
        }
        if (password == null || password.length() < 6) {
            return "La password deve contenere almeno 6 caratteri.";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            return "La password è troppo lunga (massimo 72 byte).";
        }
        if (email == null || email.isEmpty()) {
            return "L'email è obbligatoria.";
        }
        if (email.length() > MAX_EMAIL) {
            return "L'email non può superare 40 caratteri.";
        }
        if (!EMAIL.matcher(email).matches()) {
            return "L'email non è valida.";
        }
        return null;
    }

    static String validaAnagrafica(String nomeUtente, String email, String password) {
        if (nomeUtente == null || nomeUtente.isEmpty()) {
            return "Il nome utente è obbligatorio.";
        }
        if (nomeUtente.length() > MAX_NOME) {
            return "Il nome utente non può superare 100 caratteri.";
        }
        if (email == null || email.isEmpty()) {
            return "L'email è obbligatoria.";
        }
        if (email.length() > MAX_EMAIL) {
            return "L'email non può superare 40 caratteri.";
        }
        if (!EMAIL.matcher(email).matches()) {
            return "L'email non è valida.";
        }
        if (password != null) {
            if (password.length() < 6) {
                return "La password deve contenere almeno 6 caratteri.";
            }
            if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
                return "La password è troppo lunga (massimo 72 byte).";
            }
        }
        return null;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
