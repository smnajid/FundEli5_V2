import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-dealer-screen',
  template: `<h1>Dealer screen</h1>`,
})
export class DealerScreen {}
