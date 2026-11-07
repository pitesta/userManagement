package it.prgweb.service;

import it.prgweb.dto.RegisterRequest;
import it.prgweb.dto.UtenteDto;
import it.prgweb.dto.LoginRequest;
import it.prgweb.model.Utente;
import it.prgweb.repository.AppLockRepository;
import it.prgweb.repository.UtenteRepository;
import it.prgweb.util.PasswordUtil;
import it.prgweb.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Regole di autenticazione: registrazione (con valutazione "primo utente
 * amministratore" a mutex acquisito) e verifica delle credenziali.
 */
@Service
public class AuthService {

    private final UtenteRepository repository;
    private final AppLockRepository appLockRepository;

    public AuthService(UtenteRepository repository, AppLockRepository appLockRepository) {
        this.repository = repository;
        this.appLockRepository = appLockRepository;
    }

    /**
     * Registrazione serializzata sul mutex applicativo: la decisione
     * "primo utente = amministratore" è letta a lock acquisito, nella stessa
     * transazione dell'insert, senza che due registrazioni simultanee possano
     * produrre due amministratori o nessuno.
     */
    @Transactional
    public UtenteDto registra(RegisterRequest richiesta) {
        appLockRepository.acquisisciMutex();

        Optional<Utente> omonimo = repository.findByNomeUtente(richiesta.nomeUtente());
        if (omonimo.isPresent()) {
            throw ApiException.nomeGiaRegistrato();
        }

        boolean primoUtenza = repository.count() == 0;

        Utente utente = new Utente();
        utente.setNomeUtente(richiesta.nomeUtente());
        utente.setPassword(PasswordUtil.hash(richiesta.password()));
        utente.setEmail(richiesta.email());
        utente.setAmministratore(primoUtenza);
        repository.save(utente);

        return UtenteDto.of(utente);
    }

    /**
     * Verifica delle credenziali: la lettura e il confronto BCrypt avvengono
     * fuori transazione (lettura singola, nessuna scrittura, nessun mutex).
     */
    public UtenteDto login(LoginRequest richiesta) {
        Utente trovato = repository.findByNomeUtente(richiesta.nomeUtente()).orElse(null);
        if (trovato == null || !PasswordUtil.check(richiesta.password(), trovato.getPassword())) {
            throw ApiException.credenzialiNonValide();
        }
        return UtenteDto.of(trovato);
    }
}
