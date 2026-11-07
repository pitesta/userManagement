# Database H2

Il database applicativo è H2 Embedded su file, gestito da Flyway e Hibernate. H2 è single-instance: un solo processo può aprire il file alla volta (`AUTO_SERVER=FALSE`).

## Percorso e schema

`PRGWEB_DB_PATH` indica una directory, non il file. Il default è `<repo>/data`; il database è `app.mv.db`. La dipendenza H2 è la versione 2.4.240 risolta dal BOM Spring Boot 4.1.2, senza override manuali. La compatibilità tra versioni minor non è garantita: l'upgrade va pianificato e verificato prima di sostituire la versione.

Flyway applica all'avvio le migrazioni in `backend/src/main/resources/db/migration/`; V1 crea lo schema e V2 inserisce `admin` con hash BCrypt della password iniziale `admin` solo se la tabella utenti è vuota. Flyway non ripete la migrazione agli avvii successivi. Le modifiche future richiedono una nuova migrazione append-only; Hibernate usa `ddl-auto=validate` e non crea né aggiorna tabelle.

Non viene importato il precedente SQLite. Su una nuova installazione è presente soltanto l'account amministratore iniziale; le registrazioni successive sono utenti normali. I file H2 e i file temporanei sono esclusi da Git.

## Backup offline

Il build copia la versione standalone di H2 in `scripts/lib/h2.jar`. Con `AUTO_SERVER=FALSE`, H2 Shell non può collegarsi mentre Spring Boot ha aperto il database. `db-backup.sh` esegue quindi uno stop controllato, attende la terminazione e verifica che non resti un processo dell'applicazione; solo dopo esegue H2 Shell e `BACKUP TO`. Un errore di stop o backup restituisce exit code non zero. Dopo un backup riuscito l'applicazione resta ferma; riavviarla esplicitamente quando serve.

```bash
./scripts/db-backup.sh
# Output predefinito: $PRGWEB_DB_PATH/backups/prgweb-<timestamp>.zip
# Per scegliere una directory:
./scripts/db-backup.sh /percorso/backup
./scripts/start.sh  # opzionale, dopo aver verificato il backup
```

Comando H2 eseguito dallo script (il database è già chiuso):

```bash
java -cp scripts/lib/h2.jar org.h2.tools.Shell \
  -url "jdbc:h2:file:$PRGWEB_DB_PATH/app;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE" \
  -user sa -password "" \
  -sql "BACKUP TO '<dir>/prgweb-<timestamp>.zip'"
```

Lo script controlla exit code e archivio non vuoto. Il test F3 ha eseguito la procedura con l'app attiva all'inizio e ha verificato che il processo si arresti prima della connessione H2.

## Ripristino

Il ripristino va eseguito con Spring Boot fermo. Lo script rifiuta l'operazione se rileva il processo attivo, estrae in una directory temporanea e sostituisce `app.mv.db` solo dopo aver verificato il file estratto. Per default lascia l'app ferma; `--start` richiede un riavvio controllato solo dopo un ripristino riuscito.

```bash
./scripts/db-restore.sh /percorso/prgweb-<timestamp>.zip
./scripts/start.sh
# Variante esplicita:
./scripts/db-restore.sh /percorso/prgweb-<timestamp>.zip --start
```

Comando concreto di estrazione usato dallo script:

```bash
java -cp scripts/lib/h2.jar org.h2.tools.Restore \
  -file /percorso/prgweb-<timestamp>.zip \
  -dir <directory-temporanea> -db app
```

Il test F3 ha arrestato l'applicazione, creato il backup, rimosso il database, ripristinato l'archivio a processo fermo, quindi riavviato e verificato login e `GET /api/me`. Un tentativo di restore con archivio valido mentre l'app era attiva è stato rifiutato senza cambiare il file.

## Console e recupero amministratore

`db-console.sh` apre H2 Shell solo se l'applicazione è ferma; rifiuta l'accesso quando il processo è attivo. `set-admin.sh <nomeUtente> [1|0]` arresta l'app in modo controllato, verifica che il processo sia terminato, modifica il ruolo via SQL e lascia l'app ferma. Lo script impedisce la rimozione dell'ultimo amministratore.
