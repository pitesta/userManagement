package it.prgweb.web;

import it.prgweb.dao.UtenteDao;
import it.prgweb.model.Utente;
import it.prgweb.util.Json;
import it.prgweb.util.PasswordUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Login, registrazione, logout e utente corrente. */
@WebServlet(urlPatterns = {"/api/login", "/api/register", "/api/logout", "/api/me"})
public class AuthServlet extends HttpServlet {

    public static final String SESSION_USER = "utente";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_NOME = 100;
    private static final int MAX_EMAIL = 40;
    private static final int MAX_PASSWORD_BYTES = 72; // limite BCrypt

    private final UtenteDao dao = new UtenteDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("/api/me".equals(req.getServletPath())) {
            Utente utente = utenteInSessione(req);
            if (utente == null) {
                Json.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Non autenticato.");
            } else {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("utente", utente);
                Json.write(resp, HttpServletResponse.SC_OK, body);
            }
            return;
        }
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String path = req.getServletPath();
        try {
            switch (path) {
                case "/api/login" -> login(req, resp);
                case "/api/register" -> register(req, resp);
                case "/api/logout" -> logout(req, resp);
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            Json.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Richiesta non valida.");
        } catch (SQLException e) {
            log("Errore database", e);
            Json.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore interno del database.");
        }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException, SQLException {
        LoginRequest body = Json.read(req, LoginRequest.class);
        String nomeUtente = trim(body == null ? null : body.nomeUtente());
        String password = body == null ? null : body.password();

        if (nomeUtente == null || nomeUtente.isEmpty() || password == null || password.isEmpty()) {
            Json.error(resp, HttpServletResponse.SC_BAD_REQUEST,
                    "Inserire nome utente e password.");
            return;
        }

        Optional<Utente> trovato = dao.findByNomeUtente(nomeUtente);
        if (trovato.isEmpty() || !PasswordUtil.check(password, trovato.get().getPassword())) {
            Json.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Credenziali non valide.");
            return;
        }

        Utente utente = trovato.get().senzaPassword();
        HttpSession session = req.getSession(true);
        req.changeSessionId();
        session.setAttribute(SESSION_USER, utente);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("utente", utente);
        Json.write(resp, HttpServletResponse.SC_OK, result);
    }

    private void register(HttpServletRequest req, HttpServletResponse resp) throws IOException, SQLException {
        RegisterRequest body = Json.read(req, RegisterRequest.class);
        String nomeUtente = trim(body == null ? null : body.nomeUtente());
        String password = body == null ? null : body.password();
        String email = trim(body == null ? null : body.email());

        String errore = valida(nomeUtente, password, email);
        if (errore != null) {
            Json.error(resp, HttpServletResponse.SC_BAD_REQUEST, errore);
            return;
        }

        if (dao.findByNomeUtente(nomeUtente).isPresent()) {
            Json.error(resp, HttpServletResponse.SC_CONFLICT, "Nome utente già registrato.");
            return;
        }

        Utente utente = new Utente();
        utente.setNomeUtente(nomeUtente);
        utente.setPassword(PasswordUtil.hash(password));
        utente.setEmail(email);
        // Bootstrap: il primo account creato amministra l'applicazione.
        utente.setAmministratore(dao.count() == 0);
        try {
            dao.create(utente);
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("UNIQUE constraint failed")) {
                Json.error(resp, HttpServletResponse.SC_CONFLICT, "Nome utente già registrato.");
                return;
            }
            throw e;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("utente", utente.senzaPassword());
        Json.write(resp, HttpServletResponse.SC_CREATED, result);
    }

    private void logout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        Json.write(resp, HttpServletResponse.SC_OK, result);
    }

    private String valida(String nomeUtente, String password, String email) {
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

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static Utente utenteInSessione(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER);
        return value instanceof Utente utente ? utente : null;
    }

    public record LoginRequest(String nomeUtente, String password) {
    }

    public record RegisterRequest(String nomeUtente, String password, String email) {
    }
}
