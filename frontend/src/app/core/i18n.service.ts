import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { ApiError } from './models';
import { messaggioErrore } from './auth.service';

export type Lingua = 'it' | 'en' | 'fr';

export interface OpzioneLingua {
  codice: Lingua;
  nome: string;
}

export const LINGUE: OpzioneLingua[] = [
  { codice: 'it', nome: 'Italiano' },
  { codice: 'en', nome: 'English' },
  { codice: 'fr', nome: 'Français' },
];

export type ParametriTraduzione = Record<string, string | number>;

const IT: Record<string, string> = {
  'comune.lingua': 'Lingua',
  'comune.annulla': 'Annulla',
  'comune.elimina': 'Elimina',
  'comune.salva': 'Salva',
  'comune.salvataggio': 'Salvataggio…',
  'comune.caricamento': 'Caricamento…',
  'comune.chiudi': 'Chiudi',

  'menu.principale': 'Menu principale',
  'menu.home': 'Home',
  'menu.utenti': 'Utenti',
  'menu.nuovoUtente': 'Nuovo utente',
  'menu.impostazioni': 'Impostazioni',
  'shell.esci': 'Esci',

  'ruolo.amministratore': 'Amministratore',
  'ruolo.utente': 'Utente',

  'campo.idUtente': 'ID utente',
  'campo.nomeUtente': 'Nome utente',
  'campo.email': 'Email',
  'campo.password': 'Password',
  'campo.ruolo': 'Ruolo',

  'campo.nomeObbligatorio': 'Il nome utente è obbligatorio.',
  'campo.max100': 'Massimo 100 caratteri.',
  'campo.emailObbligatoria': "L'email è obbligatoria.",
  'campo.emailNonValida': "Inserisci un'email valida.",
  'campo.max40': 'Massimo 40 caratteri.',
  'campo.passwordObbligatoria': 'La password è obbligatoria.',
  'campo.min6': 'Minimo 6 caratteri.',

  'login.demo': 'Applicazione demo',
  'login.titolo1': 'Gestione utenti',
  'login.titolo2': 'semplice e sicura.',
  'login.descrizione':
    'Front-end Angular, back-end Java su Tomcat e database SQLite. Accedi con il tuo nome utente oppure registra un nuovo profilo.',
  'login.punto1': 'Password protette con hashing BCrypt',
  'login.punto2': 'Sessione autenticata lato server',
  'login.punto3': 'Menu laterale e area riservata',
  'login.accessoORegistrazione': 'Accesso o registrazione',
  'login.tabLogin': 'Login',
  'login.tabRegistrati': 'Registrati',
  'login.bentornato': 'Bentornato',
  'login.sottotitoloLogin': 'Inserisci le credenziali per entrare.',
  'login.placeholderNome': 'es. mario.rossi',
  'login.placeholderPassword': 'La tua password',
  'login.accedi': 'Accedi',
  'login.accessoInCorso': 'Accesso…',
  'login.creaAccount': 'Crea un account',
  'login.sottotitoloRegistrazione': 'Bastano nome utente, password ed email.',
  'login.placeholderScegliNome': 'Scegli un nome utente',
  'login.placeholderEmail': 'nome@esempio.it',
  'login.placeholderPasswordNuova': 'Almeno 6 caratteri',
  'login.registrati': 'Registrati',
  'login.registrazioneInCorso': 'Registrazione…',
  'login.successoPrimoAdmin':
    'Registrazione completata: sei il primo utente, quindi hai privilegi di amministratore. Ora puoi accedere.',
  'login.successoRegistrazione': 'Registrazione completata. Ora puoi accedere.',

  'home.dashboard': 'Dashboard',
  'home.ciao': 'Ciao, {nome} 👋',
  'home.utenteGenerico': 'utente',
  'home.descrizione':
    "Benvenuto nell'area riservata. Da qui puoi consultare gli utenti registrati, crearne di nuovi e gestire la tua sessione.",
  'home.cardUtentiTitolo': 'Utenti registrati',
  'home.cardUtentiTesto':
    "Consulta l'elenco completo degli account presenti nel database SQLite.",
  'home.cardUtentiLink': "Vai all'elenco →",
  'home.cardNuovoTitolo': 'Nuovo utente',
  'home.cardNuovoTesto':
    'Registra un nuovo account indicando nome utente, email e password.',
  'home.cardNuovoLink': 'Crea account →',
  'home.cardImpostazioniTitolo': 'Impostazioni',
  'home.cardImpostazioniTesto':
    "Area dedicata alle configurazioni dell'applicazione (in preparazione).",
  'home.cardImpostazioniLink': 'Apri impostazioni →',
  'home.profiloTitolo': 'Il tuo profilo',
  'home.profiloTesto': 'I dati della sessione corrente.',

  'utenti.occhiello': 'Anagrafica',
  'utenti.titolo': 'Utenti registrati',
  'utenti.descrizione': 'Elenco degli account presenti nel database.',
  'utenti.aggiorna': 'Aggiorna',
  'utenti.caricamentoElenco': 'Caricamento utenti…',
  'utenti.vuoto': 'Nessun utente registrato.',
  'utenti.colId': 'ID',
  'utenti.colNome': 'Nome utente',
  'utenti.colEmail': 'Email',
  'utenti.colRuolo': 'Ruolo',
  'utenti.colAmministratore': 'Amministratore',
  'utenti.colAzioni': 'Azioni',
  'utenti.impostaAdmin': 'Imposta come amministratore {nome}',
  'utenti.si': 'Sì',
  'utenti.no': 'No',
  'utenti.salvataggio': 'Salvataggio…',
  'utenti.modifica': 'Modifica',
  'utenti.elimina': 'Elimina',
  'utenti.totale': 'Totale: {n} utenti',
  'utenti.modificaTitolo': 'Modifica utente',
  'utenti.modificaSottotitolo': 'Aggiorna i dati di {nome}.',
  'utenti.passwordAiuto': 'Lascia vuoto per mantenere la password attuale.',
  'utenti.eliminaTitolo': 'Elimina utente',
  'utenti.eliminaConferma':
    "Confermi l'eliminazione di «{nome}»? L'operazione non può essere annullata.",
  'utenti.eliminazioneInCorso': 'Eliminazione…',

  'nuovo.occhiello': 'Anagrafica',
  'nuovo.titolo': 'Nuovo utente',
  'nuovo.descrizione':
    'Registra un account. La password viene salvata solo come hash BCrypt.',
  'nuovo.crea': 'Crea utente',
  'nuovo.successo': 'Utente «{nome}» creato correttamente.',

  'impostazioni.occhiello': 'Configurazione',
  'impostazioni.titolo': 'Impostazioni',
  'impostazioni.descrizione':
    "Riepilogo tecnico dell'applicazione e della sessione corrente.",
  'impostazioni.stack': 'Stack applicativo',
  'impostazioni.frontend': 'Front-end',
  'impostazioni.backend': 'Back-end',
  'impostazioni.server': 'Server',
  'impostazioni.database': 'Database',
  'impostazioni.sessione': 'Sessione',
  'impostazioni.utente': 'Utente',
  'impostazioni.autenticazione': 'Autenticazione',
  'impostazioni.autenticazioneValore': 'Sessione HTTP (cookie)',
  'impostazioni.passwordValore': 'Hash BCrypt',
  'impostazioni.nota':
    'Questa pagina è un segnaposto: le impostazioni modificabili verranno aggiunte nelle prossime versioni.',

  'errore.nonAutenticato': 'Non autenticato.',
  'errore.riservatoAdmin': 'Operazione riservata agli amministratori.',
  'errore.utenteNonValido': 'Utente non valido.',
  'errore.utenteNonTrovato': 'Utente non trovato.',
  'errore.ultimoAdmin': 'Deve rimanere almeno un amministratore.',
  'errore.richiestaNonValida': 'Richiesta non valida.',
  'errore.db': 'Errore interno del database.',
  'errore.credenzialiMancanti': 'Inserire nome utente e password.',
  'errore.credenzialiNonValide': 'Credenziali non valide.',
  'errore.nomeObbligatorio': 'Il nome utente è obbligatorio.',
  'errore.nomeTroppoLungo': 'Il nome utente non può superare 100 caratteri.',
  'errore.passwordCorta': 'La password deve contenere almeno 6 caratteri.',
  'errore.passwordLunga': 'La password è troppo lunga (massimo 72 byte).',
  'errore.emailObbligatoria': "L'email è obbligatoria.",
  'errore.emailTroppoLunga': "L'email non può superare 40 caratteri.",
  'errore.emailNonValida': "L'email non è valida.",
  'errore.nomeGiaRegistrato': 'Nome utente già registrato.',
  'errore.generico': 'Si è verificato un errore. Riprova.',
  'errore.serverNonRaggiungibile':
    'Server non raggiungibile. Verifica che Tomcat sia avviato.',
};

const EN: Record<string, string> = {
  'comune.lingua': 'Language',
  'comune.annulla': 'Cancel',
  'comune.elimina': 'Delete',
  'comune.salva': 'Save',
  'comune.salvataggio': 'Saving…',
  'comune.caricamento': 'Loading…',
  'comune.chiudi': 'Close',

  'menu.principale': 'Main menu',
  'menu.home': 'Home',
  'menu.utenti': 'Users',
  'menu.nuovoUtente': 'New user',
  'menu.impostazioni': 'Settings',
  'shell.esci': 'Log out',

  'ruolo.amministratore': 'Administrator',
  'ruolo.utente': 'User',

  'campo.idUtente': 'User ID',
  'campo.nomeUtente': 'Username',
  'campo.email': 'Email',
  'campo.password': 'Password',
  'campo.ruolo': 'Role',

  'campo.nomeObbligatorio': 'Username is required.',
  'campo.max100': 'Maximum 100 characters.',
  'campo.emailObbligatoria': 'Email is required.',
  'campo.emailNonValida': 'Enter a valid email address.',
  'campo.max40': 'Maximum 40 characters.',
  'campo.passwordObbligatoria': 'Password is required.',
  'campo.min6': 'Minimum 6 characters.',

  'login.demo': 'Demo application',
  'login.titolo1': 'User management',
  'login.titolo2': 'simple and secure.',
  'login.descrizione':
    'Angular front-end, Java back-end on Tomcat and SQLite database. Sign in with your username or register a new profile.',
  'login.punto1': 'Passwords protected with BCrypt hashing',
  'login.punto2': 'Server-side authenticated session',
  'login.punto3': 'Sidebar menu and restricted area',
  'login.accessoORegistrazione': 'Sign in or register',
  'login.tabLogin': 'Sign in',
  'login.tabRegistrati': 'Register',
  'login.bentornato': 'Welcome back',
  'login.sottotitoloLogin': 'Enter your credentials to sign in.',
  'login.placeholderNome': 'e.g. mario.rossi',
  'login.placeholderPassword': 'Your password',
  'login.accedi': 'Sign in',
  'login.accessoInCorso': 'Signing in…',
  'login.creaAccount': 'Create an account',
  'login.sottotitoloRegistrazione': 'Just a username, password and email.',
  'login.placeholderScegliNome': 'Choose a username',
  'login.placeholderEmail': 'name@example.com',
  'login.placeholderPasswordNuova': 'At least 6 characters',
  'login.registrati': 'Register',
  'login.registrazioneInCorso': 'Registering…',
  'login.successoPrimoAdmin':
    'Registration complete: you are the first user, so you have administrator privileges. You can now sign in.',
  'login.successoRegistrazione': 'Registration complete. You can now sign in.',

  'home.dashboard': 'Dashboard',
  'home.ciao': 'Hello, {nome} 👋',
  'home.utenteGenerico': 'user',
  'home.descrizione':
    'Welcome to the restricted area. Here you can browse registered users, create new ones and manage your session.',
  'home.cardUtentiTitolo': 'Registered users',
  'home.cardUtentiTesto':
    'Browse the complete list of accounts in the SQLite database.',
  'home.cardUtentiLink': 'Go to the list →',
  'home.cardNuovoTitolo': 'New user',
  'home.cardNuovoTesto':
    'Register a new account by entering username, email and password.',
  'home.cardNuovoLink': 'Create account →',
  'home.cardImpostazioniTitolo': 'Settings',
  'home.cardImpostazioniTesto':
    'Area for application configuration (coming soon).',
  'home.cardImpostazioniLink': 'Open settings →',
  'home.profiloTitolo': 'Your profile',
  'home.profiloTesto': 'Data from the current session.',

  'utenti.occhiello': 'Directory',
  'utenti.titolo': 'Registered users',
  'utenti.descrizione': 'List of accounts in the database.',
  'utenti.aggiorna': 'Refresh',
  'utenti.caricamentoElenco': 'Loading users…',
  'utenti.vuoto': 'No registered users.',
  'utenti.colId': 'ID',
  'utenti.colNome': 'Username',
  'utenti.colEmail': 'Email',
  'utenti.colRuolo': 'Role',
  'utenti.colAmministratore': 'Administrator',
  'utenti.colAzioni': 'Actions',
  'utenti.impostaAdmin': 'Set {nome} as administrator',
  'utenti.si': 'Yes',
  'utenti.no': 'No',
  'utenti.salvataggio': 'Saving…',
  'utenti.modifica': 'Edit',
  'utenti.elimina': 'Delete',
  'utenti.totale': 'Total: {n} users',
  'utenti.modificaTitolo': 'Edit user',
  'utenti.modificaSottotitolo': 'Update the details of {nome}.',
  'utenti.passwordAiuto': 'Leave blank to keep the current password.',
  'utenti.eliminaTitolo': 'Delete user',
  'utenti.eliminaConferma':
    'Delete “{nome}”? This action cannot be undone.',
  'utenti.eliminazioneInCorso': 'Deleting…',

  'nuovo.occhiello': 'Directory',
  'nuovo.titolo': 'New user',
  'nuovo.descrizione':
    'Register an account. The password is stored only as a BCrypt hash.',
  'nuovo.crea': 'Create user',
  'nuovo.successo': 'User “{nome}” created successfully.',

  'impostazioni.occhiello': 'Configuration',
  'impostazioni.titolo': 'Settings',
  'impostazioni.descrizione':
    'Technical overview of the application and the current session.',
  'impostazioni.stack': 'Application stack',
  'impostazioni.frontend': 'Front-end',
  'impostazioni.backend': 'Back-end',
  'impostazioni.server': 'Server',
  'impostazioni.database': 'Database',
  'impostazioni.sessione': 'Session',
  'impostazioni.utente': 'User',
  'impostazioni.autenticazione': 'Authentication',
  'impostazioni.autenticazioneValore': 'HTTP session (cookie)',
  'impostazioni.passwordValore': 'BCrypt hash',
  'impostazioni.nota':
    'This page is a placeholder: editable settings will be added in future versions.',

  'errore.nonAutenticato': 'Not authenticated.',
  'errore.riservatoAdmin': 'Operation restricted to administrators.',
  'errore.utenteNonValido': 'Invalid user.',
  'errore.utenteNonTrovato': 'User not found.',
  'errore.ultimoAdmin': 'At least one administrator must remain.',
  'errore.richiestaNonValida': 'Invalid request.',
  'errore.db': 'Internal database error.',
  'errore.credenzialiMancanti': 'Enter username and password.',
  'errore.credenzialiNonValide': 'Invalid credentials.',
  'errore.nomeObbligatorio': 'Username is required.',
  'errore.nomeTroppoLungo': 'Username cannot exceed 100 characters.',
  'errore.passwordCorta': 'Password must contain at least 6 characters.',
  'errore.passwordLunga': 'Password is too long (maximum 72 bytes).',
  'errore.emailObbligatoria': 'Email is required.',
  'errore.emailTroppoLunga': 'Email cannot exceed 40 characters.',
  'errore.emailNonValida': 'Email is not valid.',
  'errore.nomeGiaRegistrato': 'Username already registered.',
  'errore.generico': 'An error occurred. Please try again.',
  'errore.serverNonRaggiungibile':
    'Server unreachable. Check that Tomcat is running.',
};

const FR: Record<string, string> = {
  'comune.lingua': 'Langue',
  'comune.annulla': 'Annuler',
  'comune.elimina': 'Supprimer',
  'comune.salva': 'Enregistrer',
  'comune.salvataggio': 'Enregistrement…',
  'comune.caricamento': 'Chargement…',
  'comune.chiudi': 'Fermer',

  'menu.principale': 'Menu principal',
  'menu.home': 'Accueil',
  'menu.utenti': 'Utilisateurs',
  'menu.nuovoUtente': 'Nouvel utilisateur',
  'menu.impostazioni': 'Paramètres',
  'shell.esci': 'Déconnexion',

  'ruolo.amministratore': 'Administrateur',
  'ruolo.utente': 'Utilisateur',

  'campo.idUtente': 'ID utilisateur',
  'campo.nomeUtente': "Nom d'utilisateur",
  'campo.email': 'E-mail',
  'campo.password': 'Mot de passe',
  'campo.ruolo': 'Rôle',

  'campo.nomeObbligatorio': "Le nom d'utilisateur est obligatoire.",
  'campo.max100': 'Maximum 100 caractères.',
  'campo.emailObbligatoria': "L'e-mail est obligatoire.",
  'campo.emailNonValida': 'Saisissez une adresse e-mail valide.',
  'campo.max40': 'Maximum 40 caractères.',
  'campo.passwordObbligatoria': 'Le mot de passe est obligatoire.',
  'campo.min6': 'Minimum 6 caractères.',

  'login.demo': 'Application de démonstration',
  'login.titolo1': 'Gestion des utilisateurs',
  'login.titolo2': 'simple et sécurisée.',
  'login.descrizione':
    "Front-end Angular, back-end Java sur Tomcat et base de données SQLite. Connectez-vous avec votre nom d'utilisateur ou créez un nouveau profil.",
  'login.punto1': 'Mots de passe protégés par hachage BCrypt',
  'login.punto2': 'Session authentifiée côté serveur',
  'login.punto3': 'Menu latéral et espace réservé',
  'login.accessoORegistrazione': 'Connexion ou inscription',
  'login.tabLogin': 'Connexion',
  'login.tabRegistrati': 'Inscription',
  'login.bentornato': 'Bon retour',
  'login.sottotitoloLogin': 'Saisissez vos identifiants pour vous connecter.',
  'login.placeholderNome': 'ex. mario.rossi',
  'login.placeholderPassword': 'Votre mot de passe',
  'login.accedi': 'Se connecter',
  'login.accessoInCorso': 'Connexion…',
  'login.creaAccount': 'Créer un compte',
  'login.sottotitoloRegistrazione':
    "Il suffit d'un nom d'utilisateur, d'un mot de passe et d'une e-mail.",
  'login.placeholderScegliNome': "Choisissez un nom d'utilisateur",
  'login.placeholderEmail': 'nom@exemple.fr',
  'login.placeholderPasswordNuova': 'Au moins 6 caractères',
  'login.registrati': "S'inscrire",
  'login.registrazioneInCorso': 'Inscription…',
  'login.successoPrimoAdmin':
    "Inscription terminée : vous êtes le premier utilisateur, vous disposez donc des privilèges d'administrateur. Vous pouvez maintenant vous connecter.",
  'login.successoRegistrazione':
    'Inscription terminée. Vous pouvez maintenant vous connecter.',

  'home.dashboard': 'Tableau de bord',
  'home.ciao': 'Bonjour, {nome} 👋',
  'home.utenteGenerico': 'utilisateur',
  'home.descrizione':
    "Bienvenue dans l'espace réservé. Vous pouvez consulter les utilisateurs enregistrés, en créer de nouveaux et gérer votre session.",
  'home.cardUtentiTitolo': 'Utilisateurs enregistrés',
  'home.cardUtentiTesto':
    'Consultez la liste complète des comptes présents dans la base SQLite.',
  'home.cardUtentiLink': 'Aller à la liste →',
  'home.cardNuovoTitolo': 'Nouvel utilisateur',
  'home.cardNuovoTesto':
    "Enregistrez un nouveau compte en indiquant le nom d'utilisateur, l'e-mail et le mot de passe.",
  'home.cardNuovoLink': 'Créer un compte →',
  'home.cardImpostazioniTitolo': 'Paramètres',
  'home.cardImpostazioniTesto':
    "Espace dédié à la configuration de l'application (à venir).",
  'home.cardImpostazioniLink': 'Ouvrir les paramètres →',
  'home.profiloTitolo': 'Votre profil',
  'home.profiloTesto': 'Les données de la session en cours.',

  'utenti.occhiello': 'Annuaire',
  'utenti.titolo': 'Utilisateurs enregistrés',
  'utenti.descrizione': 'Liste des comptes présents dans la base de données.',
  'utenti.aggiorna': 'Actualiser',
  'utenti.caricamentoElenco': 'Chargement des utilisateurs…',
  'utenti.vuoto': 'Aucun utilisateur enregistré.',
  'utenti.colId': 'ID',
  'utenti.colNome': "Nom d'utilisateur",
  'utenti.colEmail': 'E-mail',
  'utenti.colRuolo': 'Rôle',
  'utenti.colAmministratore': 'Administrateur',
  'utenti.colAzioni': 'Actions',
  'utenti.impostaAdmin': 'Définir {nome} comme administrateur',
  'utenti.si': 'Oui',
  'utenti.no': 'Non',
  'utenti.salvataggio': 'Enregistrement…',
  'utenti.modifica': 'Modifier',
  'utenti.elimina': 'Supprimer',
  'utenti.totale': 'Total : {n} utilisateurs',
  'utenti.modificaTitolo': "Modifier l'utilisateur",
  'utenti.modificaSottotitolo': 'Mettez à jour les données de {nome}.',
  'utenti.passwordAiuto':
    'Laissez vide pour conserver le mot de passe actuel.',
  'utenti.eliminaTitolo': "Supprimer l'utilisateur",
  'utenti.eliminaConferma':
    'Confirmez-vous la suppression de « {nome} » ? Cette action est irréversible.',
  'utenti.eliminazioneInCorso': 'Suppression…',

  'nuovo.occhiello': 'Annuaire',
  'nuovo.titolo': 'Nouvel utilisateur',
  'nuovo.descrizione':
    "Enregistrez un compte. Le mot de passe n'est conservé que sous forme de hachage BCrypt.",
  'nuovo.crea': "Créer l'utilisateur",
  'nuovo.successo': 'Utilisateur « {nome} » créé correctement.',

  'impostazioni.occhiello': 'Configuration',
  'impostazioni.titolo': 'Paramètres',
  'impostazioni.descrizione':
    "Aperçu technique de l'application et de la session en cours.",
  'impostazioni.stack': 'Pile applicative',
  'impostazioni.frontend': 'Front-end',
  'impostazioni.backend': 'Back-end',
  'impostazioni.server': 'Serveur',
  'impostazioni.database': 'Base de données',
  'impostazioni.sessione': 'Session',
  'impostazioni.utente': 'Utilisateur',
  'impostazioni.autenticazione': 'Authentification',
  'impostazioni.autenticazioneValore': 'Session HTTP (cookie)',
  'impostazioni.passwordValore': 'Hachage BCrypt',
  'impostazioni.nota':
    'Cette page est un espace réservé : les paramètres modifiables seront ajoutés dans les prochaines versions.',

  'errore.nonAutenticato': 'Non authentifié.',
  'errore.riservatoAdmin': 'Opération réservée aux administrateurs.',
  'errore.utenteNonValido': 'Utilisateur non valide.',
  'errore.utenteNonTrovato': 'Utilisateur introuvable.',
  'errore.ultimoAdmin': 'Au moins un administrateur doit rester.',
  'errore.richiestaNonValida': 'Requête non valide.',
  'errore.db': 'Erreur interne de la base de données.',
  'errore.credenzialiMancanti': "Saisissez le nom d'utilisateur et le mot de passe.",
  'errore.credenzialiNonValide': 'Identifiants non valides.',
  'errore.nomeObbligatorio': "Le nom d'utilisateur est obligatoire.",
  'errore.nomeTroppoLungo':
    "Le nom d'utilisateur ne peut pas dépasser 100 caractères.",
  'errore.passwordCorta':
    'Le mot de passe doit contenir au moins 6 caractères.',
  'errore.passwordLunga':
    'Le mot de passe est trop long (maximum 72 octets).',
  'errore.emailObbligatoria': "L'e-mail est obligatoire.",
  'errore.emailTroppoLunga': "L'e-mail ne peut pas dépasser 40 caractères.",
  'errore.emailNonValida': "L'e-mail n'est pas valide.",
  'errore.nomeGiaRegistrato': "Nom d'utilisateur déjà enregistré.",
  'errore.generico': 'Une erreur est survenue. Réessayez.',
  'errore.serverNonRaggiungibile':
    'Serveur inaccessible. Vérifiez que Tomcat est démarré.',
};

const DIZIONARI: Record<Lingua, Record<string, string>> = { it: IT, en: EN, fr: FR };

/** Messaggi italiani prodotti dal back-end, mappati sulle chiavi di traduzione. */
const ERRORI_IT: Record<string, string> = {
  'Non autenticato.': 'errore.nonAutenticato',
  'Operazione riservata agli amministratori.': 'errore.riservatoAdmin',
  'Utente non valido.': 'errore.utenteNonValido',
  'Utente non trovato.': 'errore.utenteNonTrovato',
  'Deve rimanere almeno un amministratore.': 'errore.ultimoAdmin',
  'Richiesta non valida.': 'errore.richiestaNonValida',
  'Errore interno del database.': 'errore.db',
  'Inserire nome utente e password.': 'errore.credenzialiMancanti',
  'Credenziali non valide.': 'errore.credenzialiNonValide',
  'Il nome utente è obbligatorio.': 'errore.nomeObbligatorio',
  'Il nome utente non può superare 100 caratteri.': 'errore.nomeTroppoLungo',
  'La password deve contenere almeno 6 caratteri.': 'errore.passwordCorta',
  'La password è troppo lunga (massimo 72 byte).': 'errore.passwordLunga',
  "L'email è obbligatoria.": 'errore.emailObbligatoria',
  "L'email non può superare 40 caratteri.": 'errore.emailTroppoLunga',
  "L'email non è valida.": 'errore.emailNonValida',
  'Nome utente già registrato.': 'errore.nomeGiaRegistrato',
  'Si è verificato un errore. Riprova.': 'errore.generico',
  'Server non raggiungibile. Verifica che Tomcat sia avviato.':
    'errore.serverNonRaggiungibile',
};

const CHIAVE_LINGUA = 'prgweb.lingua';

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly linguaCorrente = signal<Lingua>(leggiLingua());
  readonly lingua = this.linguaCorrente.asReadonly();
  readonly dizionario = computed(() => DIZIONARI[this.linguaCorrente()]);

  constructor() {
    aggiornaDocumento(this.linguaCorrente());
  }

  t(chiave: string, parametri?: ParametriTraduzione): string {
    const testo = this.dizionario()[chiave] ?? IT[chiave] ?? chiave;
    if (!parametri) {
      return testo;
    }
    return Object.entries(parametri).reduce(
      (risultato, [nome, valore]) =>
        risultato.replaceAll(`{${nome}}`, String(valore)),
      testo,
    );
  }

  cambiaLingua(lingua: Lingua): void {
    this.linguaCorrente.set(lingua);
    salvaLingua(lingua);
    aggiornaDocumento(lingua);
  }

  /** Traduce un messaggio italiano del back-end, lasciando intatto ciò che non conosce. */
  traduciErrore(messaggio: string): string {
    const chiave = ERRORI_IT[messaggio];
    return chiave ? this.t(chiave) : messaggio;
  }

  /** Messaggio localizzato a partire da un errore HTTP. */
  errore(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      const body = error.error as ApiError | null;
      if (body && typeof body.error === 'string') {
        return this.traduciErrore(body.error);
      }
    }
    return this.traduciErrore(messaggioErrore(error));
  }
}

function leggiLingua(): Lingua {
  try {
    const salvata = globalThis.localStorage?.getItem(CHIAVE_LINGUA);
    if (salvata === 'it' || salvata === 'en' || salvata === 'fr') {
      return salvata;
    }
  } catch {
    // localStorage non disponibile: si usa l'italiano.
  }
  return 'it';
}

function salvaLingua(lingua: Lingua): void {
  try {
    globalThis.localStorage?.setItem(CHIAVE_LINGUA, lingua);
  } catch {
    // Persistenza non disponibile: la scelta resta valida per la sessione.
  }
}

function aggiornaDocumento(lingua: Lingua): void {
  if (typeof document !== 'undefined') {
    document.documentElement.lang = lingua;
  }
}
