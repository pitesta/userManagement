import { Component, OnInit, computed, inject, signal } from '@angular/core';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { I18nService } from '../../core/i18n.service';
import { Utente } from '../../core/models';

@Component({
  selector: 'app-utenti',
  imports: [ReactiveFormsModule],
  templateUrl: './utenti.html',
  styleUrl: './utenti.css',
})
export class UtentiComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);
  readonly i18n = inject(I18nService);

  readonly utenti = signal<Utente[]>([]);
  readonly caricamento = signal(true);
  readonly errore = signal('');
  readonly erroreAggiornamento = signal('');
  readonly aggiornamento = signal<number | null>(null);
  readonly sonoAmministratore = computed(() => this.auth.utente()?.amministratore === true);

  readonly utenteInModifica = signal<Utente | null>(null);
  readonly salvataggioModifica = signal(false);
  readonly erroreModifica = signal('');

  readonly utenteDaEliminare = signal<Utente | null>(null);
  readonly eliminazioneInCorso = signal(false);
  readonly erroreEliminazione = signal('');

  readonly modificaForm = this.fb.group({
    nomeUtente: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(40)]],
    password: ['', [Validators.minLength(6)]],
  });

  ngOnInit(): void {
    this.carica();
  }

  carica(): void {
    this.caricamento.set(true);
    this.errore.set('');
    this.auth.utenti().subscribe({
      next: (utenti) => {
        this.utenti.set(utenti);
        this.caricamento.set(false);
      },
      error: (err) => {
        this.errore.set(this.i18n.errore(err));
        this.caricamento.set(false);
      },
    });
  }

  cambiaAmministratore(utente: Utente): void {
    if (this.aggiornamento() !== null) {
      return;
    }
    this.erroreAggiornamento.set('');
    this.aggiornamento.set(utente.idUtente);

    this.auth.impostaAmministratore(utente.idUtente, !utente.amministratore).subscribe({
      next: (aggiornato) => {
        this.utenti.update((elenco) =>
          elenco.map((u) => (u.idUtente === aggiornato.idUtente ? aggiornato : u)),
        );
        this.aggiornamento.set(null);
      },
      error: (err) => {
        this.erroreAggiornamento.set(this.i18n.errore(err));
        this.aggiornamento.set(null);
      },
    });
  }

  apriModifica(utente: Utente): void {
    this.erroreModifica.set('');
    this.utenteInModifica.set(utente);
    this.modificaForm.reset({
      nomeUtente: utente.nomeUtente,
      email: utente.email,
      password: '',
    });
  }

  chiudiModifica(): void {
    if (this.salvataggioModifica()) {
      return;
    }
    this.utenteInModifica.set(null);
    this.erroreModifica.set('');
  }

  salvaModifica(): void {
    const utente = this.utenteInModifica();
    if (!utente || this.salvataggioModifica()) {
      return;
    }
    this.erroreModifica.set('');

    if (this.modificaForm.invalid) {
      this.modificaForm.markAllAsTouched();
      return;
    }

    const { nomeUtente, email, password } = this.modificaForm.getRawValue();
    this.salvataggioModifica.set(true);
    this.auth
      .modificaUtente(utente.idUtente, {
        nomeUtente,
        email,
        password: password.trim() ? password : undefined,
      })
      .subscribe({
        next: (aggiornato) => {
          this.utenti.update((elenco) =>
            elenco.map((u) => (u.idUtente === aggiornato.idUtente ? aggiornato : u)),
          );
          this.salvataggioModifica.set(false);
          this.utenteInModifica.set(null);
        },
        error: (err) => {
          this.erroreModifica.set(this.i18n.errore(err));
          this.salvataggioModifica.set(false);
        },
      });
  }

  modificaNonValida(campo: string, tipo: string): boolean {
    const control = this.modificaForm.get(campo);
    return !!control && control.hasError(tipo) && (control.dirty || control.touched);
  }

  chiediEliminazione(utente: Utente): void {
    this.erroreEliminazione.set('');
    this.utenteDaEliminare.set(utente);
  }

  annullaEliminazione(): void {
    if (this.eliminazioneInCorso()) {
      return;
    }
    this.utenteDaEliminare.set(null);
    this.erroreEliminazione.set('');
  }

  confermaEliminazione(): void {
    const utente = this.utenteDaEliminare();
    if (!utente || this.eliminazioneInCorso()) {
      return;
    }
    this.erroreEliminazione.set('');
    this.eliminazioneInCorso.set(true);

    this.auth.eliminaUtente(utente.idUtente).subscribe({
      next: (risposta) => {
        this.eliminazioneInCorso.set(false);
        this.utenteDaEliminare.set(null);
        if (risposta.logout) {
          void this.router.navigate(['/login']);
          return;
        }
        this.utenti.update((elenco) =>
          elenco.filter((u) => u.idUtente !== utente.idUtente),
        );
      },
      error: (err) => {
        this.erroreEliminazione.set(this.i18n.errore(err));
        this.eliminazioneInCorso.set(false);
      },
    });
  }
}
