package it.prgweb.web;

import it.prgweb.dao.UtenteDao;
import it.prgweb.model.Utente;
import it.prgweb.util.Json;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Elenco utenti registrati; richiede una sessione autenticata. */
@WebServlet(urlPatterns = "/api/utenti")
public class UtentiServlet extends HttpServlet {

    private final UtenteDao dao = new UtenteDao();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        Object value = session == null ? null : session.getAttribute(AuthServlet.SESSION_USER);
        if (!(value instanceof Utente)) {
            Json.error(resp, HttpServletResponse.SC_UNAUTHORIZED, "Non autenticato.");
            return;
        }

        try {
            List<Utente> utenti = dao.findAll().stream().map(Utente::senzaPassword).toList();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("utenti", utenti);
            Json.write(resp, HttpServletResponse.SC_OK, result);
        } catch (SQLException e) {
            log("Errore lettura utenti", e);
            Json.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Errore interno del database.");
        }
    }
}
