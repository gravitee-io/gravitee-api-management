import { Route } from '@angular/router';
import { RemoteEntry } from './entry';

export const remoteRoutes: Route[] = [
  {
    path: '',
    component: RemoteEntry,
    children: [
      {
        path: 'portal',
        loadChildren: () => import('@gravitee/gravitee-portal').then(m => m.portalRoutes),
      },
      { path: '', pathMatch: 'full', redirectTo: 'portal' },
    ],
  },
];
