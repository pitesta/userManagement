# Golden F0 — Sessione e cookie (valori misurati)

Rilevamento eseguito in F0 su Tomcat 11.0.27 con il WAR attuale, richieste HTTP reali
(`curl -i -c/-b`), database SQLite ripartito da zero. Data rilievo: 2026-11-06.

Questi valori misurati sono la fonte di verità per sessione e cookie nel nuovo backend
(design D6): qualunque default teorico cede davanti a un dato qui osservato.

## Cookie di sessione emesso al login

Header osservato su `POST /api/login` riuscito (200):

```
Set-Cookie: JSESSIONID=47A0A55ED66FED3D8BF084E0802FC33E; Path=/; HttpOnly; SameSite=Lax
```

- Nome cookie: `JSESSIONID`, valore 32 caratteri esadecimali (maiuscolo).
- **Nessun `Max-Age`/`Expires`**: cookie di sessione (scadenza a fine sessione browser;
  il limite di inattività server-side è il session-timeout qui sotto).
- `Path=/`: cookie inviato dal browser a qualunque profondità del contesto
  (verificato con richiesta riuscita su `/prgweb/api/utenti`) — requisito del proxy
  dev `:4200`.
- `HttpOnly`, `SameSite=Lax` (da `META-INF/context.xml`, `CookieProcessor sameSiteCookies="lax"`).

## Quando la sessione viene creata

- `POST /api/register` riuscito: **nessun `Set-Cookie`** (nessuna creazione di sessione).
- `POST /api/login` fallito (401): **nessun `Set-Cookie`** (nessuna creazione di sessione).
- `POST /api/login` riuscito: sessione creata e cookie emesso nella stessa risposta.
- Il cookie non è emesso con l'ID provvisorio: nella risposta compaiono un solo
  `Set-Cookie` con l'ID finale dopo `changeSessionId()`.

## Rigenerazione dell'identificatore di sessione al login

Osservato su login successivi nella stessa sessione (apiro, stesso cookie):

| Evento | JSESSIONID restituito |
| --- | --- |
| Login #1 (crea la sessione) | `47A0A55ED66FED3D8BF084E0802FC33E` |
| Login #2 con il cookie del login #1 | `F5BFA35461C2A9266A9EC2D6BEB492A4` |

Il session ID cambia a ogni login riuscito (`req.changeSessionId()`), con un solo
header `Set-Cookie` che porta l'ID nuovo. Vale anche per un nuovo login con un
cookie scaduto/invalido: il nuovo cookie sostituisce il precedente.

## Timeout di inattività

- Fonte configurativa osservata: `backend/src/main/webapp/WEB-INF/web.xml`
  `<session-timeout>30</session-timeout>` → **30 minuti** di inattività.
- Config completa osservata in web.xml: `http-only=true`, `path=/`,
  `tracking-mode=COOKIE` (nessun URL rewriting), welcome file `index.html`.

Comportamento atteso dopo il timeout: richieste col vecchio cookie →
`401 {"error":"Non autenticato."}` (non testato in tempo reale: indice = valore
configurativo; il nuovo backend usa lo stesso 30 minuti).

## Attributo di sessione

- Nome attributo: `utente`; contiene i dati utente **senza hash della password**.
- Il flag `amministratore` nel valore di sessione può non combaciare col database:
  dopo il cambio di ruolo di se stessi la servlet aggiorna l'attributo
  (misurato: admin si degrada → le richieste successive della stessa sessione
  rispondono `403 Operazione riservata agli amministratori.`; il nuovo login ripristina).
  Rispecchiare lo stesso comportamento nel nuovo backend (aggiornamento dell'utente
  di sessione sui self-update di ruolo/anagrafica).
