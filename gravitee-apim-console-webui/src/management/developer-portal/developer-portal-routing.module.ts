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

import { DeveloperPortalComponent } from './developer-portal.component';
import { DeveloperPortalTopBarComponent } from './developer-portal-top-bar/developer-portal-top-bar.component';
import { DeveloperPortalCatalogComponent } from './developer-portal-catalog/developer-portal-catalog.component';
import { DeveloperPortalApiComponent } from './developer-portal-api/developer-portal-api.component';
import { DeveloperPortalBannerComponent } from './developer-portal-banner/developer-portal-banner.component';
import { DeveloperPortalThemeComponent } from './developer-portal-theme/developer-portal-theme.component';

export const developerPortalRoutes: Routes = [
  {
    path: '',
    component: DeveloperPortalComponent,
    children: [
      {
        path: 'top-bar',
        component: DeveloperPortalTopBarComponent,
      },
      {
        path: 'catalog',
        component: DeveloperPortalCatalogComponent,
      },
      {
        path: 'api',
        component: DeveloperPortalApiComponent,
      },
      {
        path: 'banner',
        component: DeveloperPortalBannerComponent,
      },
      {
        path: 'theme',
        component: DeveloperPortalThemeComponent,
      },
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'theme',
      },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(developerPortalRoutes)],
  exports: [RouterModule],
})
export class DeveloperPortalRoutingModule {}
