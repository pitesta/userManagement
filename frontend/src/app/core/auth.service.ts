import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import {
  ApiError,
  EliminaUtenteResponse,
  ModificaUtenteRequest,
  Utente,
  UtenteResponse,
  UtentiResponse,
} from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly utenteCorrente = signal<Utente | null>(null);

  readonly utente = this.utenteCorrente.asReadonly();

  login(nomeUtente: string, password: string): Observable<Utente> {
    return this.http
      .post<UtenteResponse>('api/login', { nomeUtente, password }, { withCredentials: true })
      .pipe(
        tap((r) => this.utenteCorrente.set(r.utente)),
        map((r) => r.utente),
      );
  }

  register(nomeUtente: string, password: string, email: string): Observable<Utente> {
    return this.http
      .post<UtenteResponse>(
        'api/register',
        { nomeUtente, password, email },
        { withCredentials: true },
      )
      .pipe(map((r) => r.utente));
  }

  me(): Observable<Utente> {
    return this.http
      .get<UtenteResponse>('api/me', { withCredentials: true })
      .pipe(
        tap((r) => this.utenteCorrente.set(r.utente)),
        map((r) => r.utente),
      );
  }

  logout(): Observable<unknown> {
    return this.http
      .post('api/logout', {}, { withCredentials: true })
      .pipe(tap(() => this.utenteCorrente.set(null)));
  }

  utenti(): Observable<Utente[]> {
    return this.http
      .get<UtentiResponse>('api/utenti', { withCredentials: true })
      .pipe(map((r) => r.utenti));
  }

  impostaAmministratore(idUtente: number, amministratore: boolean): Observable<Utente> {
    return this.http
      .post<UtenteResponse>(
        'api/admin/utente',
        { idUtente, amministratore },
        { withCredentials: true },
      )
      .pipe(
        map((r) => r.utente),
        tap((utente) => {
          if (this.utenteCorrente()?.idUtente === utente.idUtente) {
            this.utenteCorrente.set(utente);
          }
        }),
      );
  }

  modificaUtente(idUtente: number, dati: ModificaUtenteRequest): Observable<Utente> {
    const corpo = dati.password
      ? { idUtente, nomeUtente: dati.nomeUtente, email: dati.email, password: dati.password }
      : { idUtente, nomeUtente: dati.nomeUtente, email: dati.email };

    return this.http
      .post<UtenteResponse>('api/admin/utente/modifica', corpo, { withCredentials: true })
      .pipe(
        map((r) => r.utente),
        tap((utente) => {
          if (this.utenteCorrente()?.idUtente === utente.idUtente) {
            this.utenteCorrente.set(utente);
          }
        }),
      );
  }

  eliminaUtente(idUtente: number): Observable<EliminaUtenteResponse> {
    return this.http
      .post<EliminaUtenteResponse>(
        'api/admin/utente/elimina',
        { idUtente },
        { withCredentials: true },
      )
      .pipe(
        tap((risposta) => {
          if (risposta.logout) {
            this.utenteCorrente.set(null);
          }
        }),
      );
  }

  dimenticaUtente(): void {
    this.utenteCorrente.set(null);
  }
}

export function messaggioErrore(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as ApiError | null;
    if (body && typeof body.error === 'string') {
      return body.error;
    }
    if (error.status === 0) {
      return 'Server non raggiungibile. Verifica che Tomcat sia avviato.';
    }
  }
  return 'Si è verificato un errore. Riprova.';
}
