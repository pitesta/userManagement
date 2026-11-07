import { TestBed } from '@angular/core/testing';
import { I18nService, LINGUE, Lingua } from './i18n.service';

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

  it('traduce gli errori noti e usa un fallback localizzato per quelli sconosciuti', () => {
    const i18n = TestBed.inject(I18nService);
    for (const lingua of ['it', 'en', 'fr'] as const) {
      i18n.cambiaLingua(lingua);
      expect(i18n.traduciErrore('Deve rimanere almeno un amministratore.')).toBe(
        i18n.t('errore.ultimoAdmin'),
      );
      expect(i18n.traduciErrore('Messaggio sconosciuto')).toBe(i18n.t('errore.generico'));
      expect(i18n.t('chiave.inesistente')).toBe(i18n.t('errore.generico'));
      expect(i18n.t('chiave.inesistente')).not.toBe('chiave.inesistente');
    }
  });

  it('mantiene dizionari completi e coerenti in italiano, inglese e francese', () => {
    const i18n = TestBed.inject(I18nService);
    const chiaviItaliane = Object.keys(i18n.dizionario()).sort();

    for (const lingua of LINGUE.map((opzione) => opzione.codice)) {
      i18n.cambiaLingua(lingua as Lingua);
      expect(Object.keys(i18n.dizionario()).sort()).toEqual(chiaviItaliane);
      expect(Object.values(i18n.dizionario()).every((valore) => valore.trim().length > 0)).toBe(true);
    }
  });

  it('localizza tutti i messaggi di errore noti e aggiorna la lingua del documento', () => {
    const i18n = TestBed.inject(I18nService);
    const erroriItaliani = [
      'Non autenticato.',
      'Operazione riservata agli amministratori.',
      'Utente non valido.',
      'Utente non trovato.',
      'Deve rimanere almeno un amministratore.',
      'Richiesta non valida.',
      'Errore interno del database.',
      'Inserire nome utente e password.',
      'Credenziali non valide.',
      'Il nome utente è obbligatorio.',
      'Il nome utente non può superare 100 caratteri.',
      'La password deve contenere almeno 6 caratteri.',
      'La password è troppo lunga (massimo 72 byte).',
      "L'email è obbligatoria.",
      "L'email non può superare 40 caratteri.",
      "L'email non è valida.",
      'Nome utente già registrato.',
      'Si è verificato un errore. Riprova.',
      'Server non raggiungibile. Verifica che Tomcat sia avviato.',
    ];

    for (const lingua of ['en', 'fr'] as const) {
      i18n.cambiaLingua(lingua);
      expect(document.documentElement.lang).toBe(lingua);
      for (const errore of erroriItaliani) {
        const tradotto = i18n.traduciErrore(errore);
        expect(tradotto).not.toBe(errore);
        expect(tradotto).not.toBe('errore.generico');
      }
    }
  });
});
