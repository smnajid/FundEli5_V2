import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'app-validation-screen',
  template: `<h1>Rule Card validation screen</h1>`,
})
export class ValidationScreen {}
