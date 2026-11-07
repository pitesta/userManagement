package it.prgweb.web;

/**
 * Eccezione di dominio con la coppia (status HTTP, messaggio in italiano)
 * del contratto REST. Sollevata dal controller o dai servizi e trasformata
 * in {"error":"..."} da GlobalExceptionHandler.
 */
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String messaggio) {
        super(messaggio);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    // ---- fabbriche per i messaggi del contratto (ERRORI_IT del frontend) ----

    public static ApiException nonAutenticato() {
        return new ApiException(401, "Non autenticato.");
    }

    public static ApiException riservatoAmministratori() {
        return new ApiException(403, "Operazione riservata agli amministratori.");
    }

    public static ApiException utenteNonValido() {
        return new ApiException(400, "Utente non valido.");
    }

    public static ApiException utenteNonTrovato() {
        return new ApiException(404, "Utente non trovato.");
    }

    public static ApiException ultimoAmministratore() {
        return new ApiException(409, "Deve rimanere almeno un amministratore.");
    }

    public static ApiException richiestaNonValida() {
        return new ApiException(400, "Richiesta non valida.");
    }

    public static ApiException credenzialiMancanti() {
        return new ApiException(400, "Inserire nome utente e password.");
    }

    public static ApiException credenzialiNonValide() {
        return new ApiException(401, "Credenziali non valide.");
    }

    public static ApiException nomeObbligatorio() {
        return new ApiException(400, "Il nome utente è obbligatorio.");
    }

    public static ApiException nomeTroppoLungo() {
        return new ApiException(400, "Il nome utente non può superare 100 caratteri.");
    }

    public static ApiException passwordCorta() {
        return new ApiException(400, "La password deve contenere almeno 6 caratteri.");
    }

    public static ApiException passwordTroppoLunga() {
        return new ApiException(400, "La password è troppo lunga (massimo 72 byte).");
    }

    public static ApiException emailObbligatoria() {
        return new ApiException(400, "L'email è obbligatoria.");
    }

    public static ApiException emailTroppoLunga() {
        return new ApiException(400, "L'email non può superare 40 caratteri.");
    }

    public static ApiException emailNonValida() {
        return new ApiException(400, "L'email non è valida.");
    }

    public static ApiException nomeGiaRegistrato() {
        return new ApiException(409, "Nome utente già registrato.");
    }

    public static ApiException erroreDatabase() {
        return new ApiException(500, "Errore interno del database.");
    }
}
