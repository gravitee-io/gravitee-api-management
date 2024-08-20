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
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { PortalSettingsModule } from './portal-settings.module';
import { PortalNavigationComponent } from './navigation/portal-navigation.component';
import { PortalCustomizationComponent } from './customization/portal-customization.component';
import { PortalBannerComponent } from './customization/banner/portal-banner.component';
import { PortalThemeComponent } from './customization/theme/portal-theme.component';

import { PermissionGuard } from '../shared/components/gio-permission/gio-permission.guard';
import { HasLicenseGuard } from '../shared/components/gio-license/has-license.guard';
import { EnvironmentGuard } from '../management/environment.guard';

const portalRoutes: Routes = [
  {
    path: '',
    component: PortalNavigationComponent,
    canActivate: [EnvironmentGuard.initEnvConfigAndLoadPermissions],
    canActivateChild: [HasLicenseGuard, PermissionGuard.checkRouteDataPermissions],
    children: [
      {
        path: 'customization',
        component: PortalCustomizationComponent,
        children: [
          {
            path: 'banner',
            component: PortalBannerComponent,
            data: {
              permissions: {
                anyOf: ['environment-settings-r', 'environment-settings-u'],
              },
            },
          },
          {
            path: 'theme',
            component: PortalThemeComponent,
            data: {
              permissions: {
                anyOf: ['environment-theme-r', 'environment-theme-u'],
              },
            },
          },
          {
            path: '',
            pathMatch: 'full',
            redirectTo: 'theme',
          },
        ],
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'customization',
      },
    ],
  },
];

@NgModule({
  imports: [PortalSettingsModule, RouterModule.forChild(portalRoutes)],
  exports: [RouterModule],
})
export class PortalSettingsRoutingModule {}
