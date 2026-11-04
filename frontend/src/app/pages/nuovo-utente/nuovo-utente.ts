import { Component, inject, signal } from '@angular/core';
import {
  NonNullableFormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { AuthService } from '../../core/auth.service';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-nuovo-utente',
  imports: [ReactiveFormsModule],
  templateUrl: './nuovo-utente.html',
  styleUrl: './nuovo-utente.css',
})
export class NuovoUtenteComponent {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);
  readonly i18n = inject(I18nService);

  readonly caricamento = signal(false);
  readonly errore = signal('');
  readonly successo = signal('');

  readonly form = this.fb.group({
    nomeUtente: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(40)]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  salva(): void {
    this.errore.set('');
    this.successo.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.caricamento.set(true);
    const { nomeUtente, email, password } = this.form.getRawValue();
    this.auth.register(nomeUtente, password, email).subscribe({
      next: (utente) => {
        this.caricamento.set(false);
        this.form.reset();
        this.successo.set(
          this.i18n.t('nuovo.successo', { nome: utente.nomeUtente }),
        );
      },
      error: (err) => {
        this.caricamento.set(false);
        this.errore.set(this.i18n.errore(err));
      },
    });
  }

  nonValido(campo: string, tipo: string): boolean {
    const control = this.form.get(campo);
    return !!control && control.hasError(tipo) && (control.dirty || control.touched);
  }
}
