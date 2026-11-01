import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <nav>
      <a routerLink="/dealer-screen" routerLinkActive="active">Dealer</a>
      <a routerLink="/validation-screen" routerLinkActive="active">Validation</a>
    </nav>
    <router-outlet />
  `,
})
export class App {}
