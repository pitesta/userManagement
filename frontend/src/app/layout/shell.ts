import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { I18nService } from '../core/i18n.service';

interface VoceMenu {
  path: string;
  chiave: string;
  icona: string;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.css',
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  readonly utente = this.auth.utente;

  readonly menu: VoceMenu[] = [
    { path: '/home', chiave: 'menu.home', icona: '⌂' },
    { path: '/utenti', chiave: 'menu.utenti', icona: '☰' },
    { path: '/nuovo-utente', chiave: 'menu.nuovoUtente', icona: '+' },
    { path: '/impostazioni', chiave: 'menu.impostazioni', icona: '⚙' },
  ];

  logout(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login']),
    });
  }
}
