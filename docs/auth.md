# Autenticazione e sessioni

L'autenticazione è server-side con `HttpSession` e cookie `JSESSIONID`; non usa JWT. La sessione contiene un `UtenteSessione` senza hash password.

## Cookie misurato in F0

| Proprietà | Valore |
|---|---|
| Nome | `JSESSIONID` |
| Path | `/` |
| HttpOnly | sì |
| SameSite | `Lax` |
| Timeout inattività | 30 minuti |
| Tracking | solo cookie |
| Rigenerazione ID | a ogni login riuscito con `changeSessionId()` |

La registrazione e il login fallito non creano una sessione. Logout invalida la sessione se presente. Le impostazioni sono in `application.yml`; i valori sono quelli misurati nel golden `openspec/changes/migrate-backend-to-spring-boot-jpa-h2/golden/f0-sessione.md`.

## Autorizzazioni

- `/api/register`, `/api/login`, `/api/logout`: pubblici.
- `/api/me` e `GET /api/utenti`: richiedono una sessione autenticata.
- Le tre operazioni `/api/admin/utente...` richiedono sessione e ruolo amministratore.
- Una nuova installazione contiene l'account `admin` iniziale; le successive registrazioni non sono amministratori. In un database vuoto senza utenti, il primo account registrato diventa admin.
- L'ultimo admin non può essere degradato o eliminato.
- Self-update di ruolo/anagrafica aggiorna la sessione; self-delete la invalida.

I controller sono in `web/`; la logica di dominio è nei service. L'hash BCrypt non viene restituito dalle API.

