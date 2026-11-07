import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../core/i18n.service';
import { TopbarComponent } from './topbar';

describe('TopbarComponent', () => {
  let fixture: ComponentFixture<TopbarComponent>;
  let i18n: I18nService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ imports: [TopbarComponent] });
    fixture = TestBed.createComponent(TopbarComponent);
    i18n = TestBed.inject(I18nService);
    fixture.detectChanges();
  });

  it('mostra il selettore italiano, inglese e francese e aggiorna la lingua', () => {
    const select: HTMLSelectElement = fixture.nativeElement.querySelector('select');
    expect(Array.from(select.options).map((option) => option.value)).toEqual(['it', 'en', 'fr']);

    select.value = 'en';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(i18n.lingua()).toBe('en');
    expect(fixture.nativeElement.textContent).toContain('User Management');
    expect(document.documentElement.lang).toBe('en');

    select.value = 'fr';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(i18n.lingua()).toBe('fr');
    expect(fixture.nativeElement.textContent).toContain('Gestion des utilisateurs');
    expect(document.documentElement.lang).toBe('fr');
  });
});
