import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dealer-screen' },
  {
    path: 'dealer-screen',
    loadComponent: () => import('./dealer-screen/dealer-screen').then((m) => m.DealerScreen),
  },
  {
    path: 'validation-screen',
    loadComponent: () =>
      import('./validation-screen/validation-screen').then((m) => m.ValidationScreen),
  },
];
