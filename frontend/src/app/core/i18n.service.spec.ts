import { TestBed } from '@angular/core/testing';
import { I18nService } from './i18n.service';

describe('I18nService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
  });

  it('parte dall\'italiano', () => {
    const i18n = TestBed.inject(I18nService);
    expect(i18n.lingua()).toBe('it');
    expect(i18n.t('menu.utenti')).toBe('Utenti');
  });

  it('cambia lingua e la ricorda', () => {
    const i18n = TestBed.inject(I18nService);

    i18n.cambiaLingua('en');
    expect(i18n.lingua()).toBe('en');
    expect(i18n.t('menu.utenti')).toBe('Users');
    expect(localStorage.getItem('prgweb.lingua')).toBe('en');

    i18n.cambiaLingua('fr');
    expect(i18n.t('menu.utenti')).toBe('Utilisateurs');
  });

  it('interpola i parametri', () => {
    const i18n = TestBed.inject(I18nService);
    expect(i18n.t('utenti.totale', { n: 3 })).toBe('Totale: 3 utenti');
  });

  it('traduce i messaggi di errore del back-end e lascia gli altri invariati', () => {
    const i18n = TestBed.inject(I18nService);
    i18n.cambiaLingua('fr');

    expect(i18n.traduciErrore('Deve rimanere almeno un amministratore.')).toBe(
      'Au moins un administrateur doit rester.',
    );
    expect(i18n.traduciErrore('Messaggio sconosciuto')).toBe('Messaggio sconosciuto');
  });
});
