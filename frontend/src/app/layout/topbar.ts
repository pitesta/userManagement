import { Component, inject } from '@angular/core';
import { I18nService, LINGUE, Lingua } from '../core/i18n.service';

@Component({
  selector: 'app-topbar',
  templateUrl: './topbar.html',
  styleUrl: './topbar.css',
})
export class TopbarComponent {
  readonly i18n = inject(I18nService);
  readonly lingue = LINGUE;

  cambiaLingua(event: Event): void {
    const valore = (event.target as HTMLSelectElement).value as Lingua;
    this.i18n.cambiaLingua(valore);
  }
}
