import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/auth.service';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-impostazioni',
  templateUrl: './impostazioni.html',
  styleUrl: './impostazioni.css',
})
export class ImpostazioniComponent {
  private readonly auth = inject(AuthService);
  readonly i18n = inject(I18nService);
  readonly utente = this.auth.utente;
}
