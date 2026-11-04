export interface Utente {
  idUtente: number;
  nomeUtente: string;
  email: string;
  amministratore: boolean;
}

export interface UtenteResponse {
  utente: Utente;
}

export interface UtentiResponse {
  utenti: Utente[];
}

export interface ApiError {
  error?: string;
}

export interface ModificaUtenteRequest {
  nomeUtente: string;
  email: string;
  password?: string;
}

export interface EliminaUtenteResponse {
  ok: boolean;
  logout?: boolean;
}
