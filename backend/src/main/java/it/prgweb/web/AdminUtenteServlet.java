package it.prgweb.web;

import it.prgweb.dao.UtenteDao;
import it.prgweb.model.Utente;
import it.prgweb.util.Json;
import it.prgweb.util.PasswordUtil;
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

/**
 * Operazioni riservate agli amministratori: cambio ruolo, modifica
 * dell'anagrafica ed eliminazione di un utente.
 */
@WebServlet(urlPatterns = {
        "/api/admin/utente",
        "/api/admin/utente/modifica",
        "/api/admin/utente/elimina"
})
public class AdminUtenteServlet extends HttpServlet {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_NOME = 100;
    private static final int MAX_EMAIL = 40;
    private static final int MAX_PASSWORD_BYTES = 72; // limite BCrypt

    private final UtenteDao dao = new UtenteDao();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        Object value = session == null ? null : session.getAttribute(AuthServlet.SESSION_USER);
        Utente corrente = value instanceof Utente utente ? utente : null;

        if (corrente == null) {
            Json.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Non autenticato.");
            return;
        }
        if (!corrente.isAmministratore()) {
            Json.error(resp, HttpServletResponse.SC_FORBIDDEN,
                    "Operazione riservata agli amministratori.");
            return;
        }

        try {
            switch (req.getServletPath()) {
                case "/api/admin/utente" -> cambiaRuolo(req, resp, session, corrente);
                case "/api/admin/utente/modifica" -> modifica(req, resp, session, corrente);
                case "/api/admin/utente/elimina" -> elimina(req, resp, session, corrente);
                default -> resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            Json.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Richiesta non valida.");
        } catch (SQLException e) {
            log("Errore operazione amministratore", e);
            Json.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore interno del database.");
        }
    }

    private void cambiaRuolo(HttpServletRequest req, HttpServletResponse resp,
                             HttpSession session, Utente corrente)
            throws IOException, SQLException {
        Richiesta body = Json.read(req, Richiesta.class);
        if (body == null || body.idUtente() <= 0) {
            Json.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Utente non valido.");
            return;
        }

        Optional<Utente> trovato = dao.findById(body.idUtente());
        if (trovato.isEmpty()) {
            Json.error(resp, HttpServletResponse.SC_NOT_FOUND, "Utente non trovato.");
            return;
        }

        Utente target = trovato.get();
        if (target.isAmministratore() && !body.amministratore()
                && dao.countAmministratori() <= 1) {
            Json.error(resp, HttpServletResponse.SC_CONFLICT,
                    "Deve rimanere almeno un amministratore.");
            return;
        }

        if (!dao.updateAmministratore(target.getIdUtente(), body.amministratore())) {
            Json.error(resp, HttpServletResponse.SC_NOT_FOUND, "Utente non trovato.");
            return;
        }
        target.setAmministratore(body.amministratore());

        Utente aggiornato = target.senzaPassword();
        if (aggiornato.getIdUtente() == corrente.getIdUtente()) {
            session.setAttribute(AuthServlet.SESSION_USER, aggiornato);
        }

        rispostaUtente(resp, aggiornato);
    }

    private void modifica(HttpServletRequest req, HttpServletResponse resp,
                          HttpSession session, Utente corrente)
            throws IOException, SQLException {
        ModificaRequest body = Json.read(req, ModificaRequest.class);
        if (body == null || body.idUtente() <= 0) {
            Json.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Utente non valido.");
            return;
        }

        String nomeUtente = trim(body.nomeUtente());
        String email = trim(body.email());
        String password = body.password();
        boolean cambiaPassword = password != null && !password.isBlank();

        String errore = valida(nomeUtente, email, cambiaPassword ? password : null);
        if (errore != null) {
            Json.error(resp, HttpServletResponse.SC_BAD_REQUEST, errore);
            return;
        }

        Optional<Utente> trovato = dao.findById(body.idUtente());
        if (trovato.isEmpty()) {
            Json.error(resp, HttpServletResponse.SC_NOT_FOUND, "Utente non trovato.");
            return;
        }

        Optional<Utente> omonimo = dao.findByNomeUtente(nomeUtente);
        if (omonimo.isPresent() && omonimo.get().getIdUtente() != body.idUtente()) {
            Json.error(resp, HttpServletResponse.SC_CONFLICT, "Nome utente già registrato.");
            return;
        }

        Utente target = trovato.get();
        target.setNomeUtente(nomeUtente);
        target.setEmail(email);
        if (cambiaPassword) {
            target.setPassword(PasswordUtil.hash(password));
        }

        try {
            if (!dao.update(target)) {
                Json.error(resp, HttpServletResponse.SC_NOT_FOUND, "Utente non trovato.");
                return;
            }
        } catch (SQLException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.contains("UNIQUE constraint failed")) {
                Json.error(resp, HttpServletResponse.SC_CONFLICT,
                        "Nome utente già registrato.");
                return;
            }
            throw e;
        }

        Utente aggiornato = target.senzaPassword();
        if (aggiornato.getIdUtente() == corrente.getIdUtente()) {
            session.setAttribute(AuthServlet.SESSION_USER, aggiornato);
        }

        rispostaUtente(resp, aggiornato);
    }

    private void elimina(HttpServletRequest req, HttpServletResponse resp,
                         HttpSession session, Utente corrente)
            throws IOException, SQLException {
        EliminaRequest body = Json.read(req, EliminaRequest.class);
        if (body == null || body.idUtente() <= 0) {
            Json.error(resp, HttpServletResponse.SC_BAD_REQUEST, "Utente non valido.");
            return;
        }

        Optional<Utente> trovato = dao.findById(body.idUtente());
        if (trovato.isEmpty()) {
            Json.error(resp, HttpServletResponse.SC_NOT_FOUND, "Utente non trovato.");
            return;
        }

        Utente target = trovato.get();
        if (target.isAmministratore() && dao.countAmministratori() <= 1) {
            Json.error(resp, HttpServletResponse.SC_CONFLICT,
                    "Deve rimanere almeno un amministratore.");
            return;
        }

        if (!dao.deleteById(target.getIdUtente())) {
            Json.error(resp, HttpServletResponse.SC_NOT_FOUND, "Utente non trovato.");
            return;
        }

        boolean logout = target.getIdUtente() == corrente.getIdUtente();
        if (logout) {
            session.invalidate();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("logout", logout);
        Json.write(resp, HttpServletResponse.SC_OK, result);
    }

    private void rispostaUtente(HttpServletResponse resp, Utente utente) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("utente", utente);
        Json.write(resp, HttpServletResponse.SC_OK, result);
    }

    private String valida(String nomeUtente, String email, String password) {
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

    public record Richiesta(long idUtente, boolean amministratore) {
    }

    public record ModificaRequest(long idUtente, String nomeUtente, String email, String password) {
    }

    public record EliminaRequest(long idUtente) {
    }
}
