/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Routes } from '@angular/router';

import { HomepageComponent } from '../../portal/homepage/homepage.component';
import { EnvironmentGuard } from '../../management/environment.guard';
import { HasLicenseGuard } from '../../shared/components/gio-license/has-license.guard';
import { PermissionGuard } from '../../shared/components/gio-permission/gio-permission.guard';
import { HasUnsavedChangesGuard } from '../../shared/guards/has-unsaved-changes.guard';

export const gammaPortalRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'default',
  },
  {
    path: ':envHrid',
    canActivate: [EnvironmentGuard.initEnvConfigAndLoadPermissions],
    canActivateChild: [HasLicenseGuard, PermissionGuard.checkRouteDataPermissions],
    children: [
      {
        path: 'homepage',
        component: HomepageComponent,
        canDeactivate: [HasUnsavedChangesGuard],
        data: {
          permissions: {
            anyOf: ['environment-documentation-r', 'environment-documentation-u'],
          },
        },
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'homepage',
      },
    ],
  },
];
