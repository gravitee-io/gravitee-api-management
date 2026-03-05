import { Route } from '@angular/router';

import { HasUnsavedChangesGuard } from '../utils/has-unsaved-changes.guard';

export const portalRoutes: Route[] = [
  {
    path: 'navigation',
    loadComponent: () =>
      import('./navigation-items/portal-navigation-items.component').then(m => m.PortalNavigationItemsComponent),
    canDeactivate: [HasUnsavedChangesGuard],
  },
  {
    path: 'theme',
    loadComponent: () => import('./theme/portal-theme.component').then(m => m.PortalThemeComponent),
  },
  {
    path: 'homepage',
    loadComponent: () => import('./homepage/homepage.component').then(m => m.HomepageComponent),
    canDeactivate: [HasUnsavedChangesGuard],
  },
  {
    path: 'subscription-form',
    loadComponent: () => import('./subscription-form/subscription-form.component').then(m => m.SubscriptionFormComponent),
    canDeactivate: [HasUnsavedChangesGuard],
  },
  { path: '', pathMatch: 'full', redirectTo: 'navigation' },
];
