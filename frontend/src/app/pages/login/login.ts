import { Component, OnInit, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { I18nService } from '../../core/i18n.service';

type Modalita = 'login' | 'registrazione';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class LoginComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(NonNullableFormBuilder);
  readonly i18n = inject(I18nService);

  readonly modalita = signal<Modalita>('login');
  readonly caricamento = signal(false);
  readonly errore = signal('');
  readonly avviso = signal('');

  readonly loginForm = this.fb.group({
    nomeUtente: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  readonly registrazioneForm = this.fb.group({
    nomeUtente: ['', [Validators.required, Validators.maxLength(100)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(40)]],
  });

  ngOnInit(): void {
    if (this.auth.utente()) {
      void this.router.navigate(['/home']);
    }
  }

  cambiaModalita(modalita: Modalita): void {
    this.modalita.set(modalita);
    this.errore.set('');
    this.avviso.set('');
  }

  accedi(): void {
    this.errore.set('');
    this.avviso.set('');
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.caricamento.set(true);
    const { nomeUtente, password } = this.loginForm.getRawValue();
    this.auth.login(nomeUtente, password).subscribe({
      next: () => {
        this.caricamento.set(false);
        void this.router.navigate(['/home']);
      },
      error: (err) => {
        this.caricamento.set(false);
        this.errore.set(this.i18n.errore(err));
      },
    });
  }

  registrati(): void {
    this.errore.set('');
    this.avviso.set('');
    if (this.registrazioneForm.invalid) {
      this.registrazioneForm.markAllAsTouched();
      return;
    }

    this.caricamento.set(true);
    const { nomeUtente, password, email } = this.registrazioneForm.getRawValue();
    this.auth.register(nomeUtente, password, email).subscribe({
      next: (utente) => {
        this.caricamento.set(false);
        this.registrazioneForm.reset();
        this.loginForm.patchValue({ nomeUtente, password: '' });
        this.modalita.set('login');
        this.avviso.set(
          this.i18n.t(
            utente.amministratore ? 'login.successoPrimoAdmin' : 'login.successoRegistrazione',
          ),
        );
      },
      error: (err) => {
        this.caricamento.set(false);
        this.errore.set(this.i18n.errore(err));
      },
    });
  }

  campoNonValido(form: 'login' | 'registrazione', campo: string, tipo: string): boolean {
    const control =
      form === 'login'
        ? this.loginForm.get(campo)
        : this.registrazioneForm.get(campo);
    return !!control && control.hasError(tipo) && (control.dirty || control.touched);
  }
}
