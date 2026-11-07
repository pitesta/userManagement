package it.prgweb.repository;

import it.prgweb.model.AppLock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/**
 * Repository custom con accesso diretto via EntityManager: acquisizione del
 * mutex applicativo con SELECT ... FOR UPDATE sulla riga permanente
 * app_lock id=1 (design D5). DEVE essere chiamata dentro una transazione
 * attiva: il lock è acquisito nella stessa transazione che verifica le
 * invarianti e scrive, non in una transazione separata.
 */
@Repository
public class AppLockRepository {

    @PersistenceContext
    private EntityManager em;

    /** Acquisisce il mutex applicativo bloccando la riga app_lock id=1. */
    public void acquisisciMutex() {
        em.createQuery("select a from AppLock a where a.id = 1", AppLock.class)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getSingleResult();
    }
}
