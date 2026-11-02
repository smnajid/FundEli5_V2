import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <nav class="topbar" aria-label="Screens">
      <span class="brand">FundEli5</span>
      <a routerLink="/dealer-screen" routerLinkActive="active">Dealer</a>
      <a routerLink="/validation-screen" routerLinkActive="active">Validation</a>
      <span class="role">Fund Dealer</span>
    </nav>
    <main class="page">
      <router-outlet />
    </main>
  `,
})
export class App {}
