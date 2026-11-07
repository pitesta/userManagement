package it.prgweb.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Riga tecnica del mutex applicativo (design D5): una sola riga permanente
 * con id=1, creata da Flyway V1 e mai modificata.
 */
@Entity
@Table(name = "app_lock")
public class AppLock {

    @Id
    @Column(name = "id")
    private long id;

    @Column(name = "descrizione")
    private String descrizione;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }
}
