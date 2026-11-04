import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TopbarComponent } from './layout/topbar';

@Component({
  imports: [RouterOutlet, TopbarComponent],
  selector: 'app-root',
  template: '<app-topbar /><router-outlet />',
  styles: `
    :host {
      display: block;
      min-height: 100dvh;
    }
  `,
})
export class App {}
