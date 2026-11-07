package it.prgweb.service;

import it.prgweb.dto.EliminaRequest;
import it.prgweb.dto.ModificaRequest;
import it.prgweb.dto.CambioRuoloRequest;
import it.prgweb.dto.UtenteDto;
import it.prgweb.model.Utente;
import it.prgweb.repository.AppLockRepository;
import it.prgweb.repository.UtenteRepository;
import it.prgweb.util.PasswordUtil;
import it.prgweb.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operazioni su utenti: elenco e regole amministrative (cambio ruolo,
 * modifica anagrafica, eliminazione) con le invarianti "almeno un
 * amministratore" valutate a mutex acquisito (design D5).
 */
@Service
public class UtenteService {

    private final UtenteRepository repository;
    private final AppLockRepository appLockRepository;

    public UtenteService(UtenteRepository repository, AppLockRepository appLockRepository) {
        this.repository = repository;
        this.appLockRepository = appLockRepository;
    }

    /** Elenco completo ordinato per nome utente (case-insensitive); senza mutex. */
    public List<UtenteDto> elenco() {
        return repository.elenco().stream().map(UtenteDto::of).toList();
    }

    /** Cambio ruolo con invariante ultimo-admin valutata sul mutex acquisito. */
    @Transactional
    public UtenteDto cambiaRuolo(CambioRuoloRequest richiesta) {
        appLockRepository.acquisisciMutex();

        Utente target = repository.findById(richiesta.idUtente())
                .orElseThrow(ApiException::utenteNonTrovato);

        boolean degrada = target.isAmministratore() && !richiesta.amministratore();
        if (degrada && repository.countByAmministratoreIsTrue() <= 1) {
            throw ApiException.ultimoAmministratore();
        }
        target.setAmministratore(richiesta.amministratore());

        return UtenteDto.of(target);
    }

    /** Modifica anagrafica (nome, email, password opzionale); nome unico. */
    @Transactional
    public UtenteDto modifica(ModificaRequest richiesta) {
        Utente target = repository.findById(richiesta.idUtente())
                .orElseThrow(ApiException::utenteNonTrovato);

        // Blocca l'omonimo osservato; una race comunque esca viene respinta
        // dal vincolo UNIQUE con lo stesso messaggio (GlobalExceptionHandler).
        Utente omonimo = repository.findByNomeUtente(richiesta.nomeUtente()).orElse(null);
        if (omonimo != null && omonimo.getIdUtente() != richiesta.idUtente()) {
            throw ApiException.nomeGiaRegistrato();
        }

        target.setNomeUtente(richiesta.nomeUtente());
        target.setEmail(richiesta.email());
        if (richiesta.password() != null && !richiesta.password().isBlank()) {
            target.setPassword(PasswordUtil.hash(richiesta.password()));
        }

        return UtenteDto.of(target);
    }

    /** Eliminazione con invariante ultimo-admin valutata sul mutex acquisito. */
    @Transactional
    public void elimina(EliminaRequest richiesta) {
        appLockRepository.acquisisciMutex();

        Utente target = repository.findById(richiesta.idUtente())
                .orElseThrow(ApiException::utenteNonTrovato);

        if (target.isAmministratore() && repository.countByAmministratoreIsTrue() <= 1) {
            throw ApiException.ultimoAmministratore();
        }
        repository.delete(target);
    }
}
