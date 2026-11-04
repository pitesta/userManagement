package it.prgweb.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/** Utente dell'applicazione. La password è sempre un hash BCrypt. */
public class Utente implements Serializable {

    private long idUtente;
    private String nomeUtente;
    private String password;
    private String email;
    private boolean amministratore;

    public long getIdUtente() {
        return idUtente;
    }

    public void setIdUtente(long idUtente) {
        this.idUtente = idUtente;
    }

    public String getNomeUtente() {
        return nomeUtente;
    }

    public void setNomeUtente(String nomeUtente) {
        this.nomeUtente = nomeUtente;
    }

    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAmministratore() {
        return amministratore;
    }

    public void setAmministratore(boolean amministratore) {
        this.amministratore = amministratore;
    }

    /** Copia senza hash, adatta alla sessione e alle risposte JSON. */
    public Utente senzaPassword() {
        Utente u = new Utente();
        u.setIdUtente(idUtente);
        u.setNomeUtente(nomeUtente);
        u.setEmail(email);
        u.setAmministratore(amministratore);
        return u;
    }
}
