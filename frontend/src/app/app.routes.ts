import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { ShellComponent } from './layout/shell';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then((m) => m.LoginComponent),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'home', pathMatch: 'full' },
      {
        path: 'home',
        loadComponent: () => import('./pages/home/home').then((m) => m.HomeComponent),
      },
      {
        path: 'utenti',
        loadComponent: () => import('./pages/utenti/utenti').then((m) => m.UtentiComponent),
      },
      {
        path: 'nuovo-utente',
        loadComponent: () =>
          import('./pages/nuovo-utente/nuovo-utente').then((m) => m.NuovoUtenteComponent),
      },
      {
        path: 'impostazioni',
        loadComponent: () =>
          import('./pages/impostazioni/impostazioni').then((m) => m.ImpostazioniComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
